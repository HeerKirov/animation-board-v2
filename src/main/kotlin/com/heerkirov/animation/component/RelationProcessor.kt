package com.heerkirov.animation.component

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.enums.RelationType
import com.heerkirov.animation.util.relation.RelationGraph
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class RelationProcessor(@Autowired private val database: Database) {
    /**
     * 对单个animation的relation进行更新。
     * 首先根据旧的全量拓扑，找出所有关联对象。然后加入新的拓扑关联对象，构成全量图。
     * 然后，对全量图进行关系传播推导，导出所有对象的全量拓扑，并更新那些拓扑发生变化的对象。
     * @throws NoSuchElementException 找不到指定id的animation。
     */
    fun updateRelationTopology(animationId: Int, newRelations: Map<RelationType, List<Int>>) {
        //查到主对象
        val thisAnimation = find(animationId) ?: throw NoSuchElementException("Cannot find animation $animationId.")

        //比对主对象的旧关联拓扑和新关联拓扑，找出新增的那些节点
        //而如果关联拓扑没有变化，那么退出这个方法
        val changes = compareRelationAdds(thisAnimation.relations, newRelations)
        if(changes.isEmpty() && compareRelationEquals(thisAnimation.relations, newRelations)) {
            return
        }

        //节点列表
        val elements = hashMapOf<Int, AnimationModel>()
        //将主对象放入图中
        elements[animationId] = thisAnimation
        //将主对象的旧的全量拓扑的关联节点放入图中
        elements.putAll(findAll(thisAnimation.relationsTopology.flatMap { (_, v) -> v }).map { Pair(it.id, it) })

        //查找上述拓扑比对结果中新增节点，将它们放入图中。在那之前，计算id和exists的差以减少查询
        val changesMinusExists = changes - elements.keys
        val appendAnimations = findAll(changesMinusExists)
        elements.putAll(appendAnimations.map { Pair(it.id, it) })

        //查找上述拓扑比对结果中新增节点的全量拓扑，将它们也都放入图中。在那之前，计算id和exists的差以减少查询
        val changesTopologyIds = appendAnimations.flatMap { it.relationsTopology.flatMap { (_, v) -> v } }
        val changesTopologyIdsMinusExists = changesTopologyIds - elements.keys
        val changesTopologyAnimations = findAll(changesTopologyIdsMinusExists)
        elements.putAll(changesTopologyAnimations.map { Pair(it.id, it) })

        //根据所有在场的节点的关联拓扑(主对象的为新关联拓扑)，将关系放入图中，随后构建传播图
        val graph = RelationGraph<AnimationModel, RelationType>(elements.values.toTypedArray()) {
            for (animation in elements.values) {
                for ((relation, list) in animation.relations.entries) {
                    for (i in list) {
                        addRelation(animation, relation, elements[i]!!)
                    }
                }
            }
        }

        //从传播图导出每一个节点的全量拓扑
        //比对每个节点的新旧全量拓扑，发生变化的放入保存列表；主对象要更新关联拓扑，也要放入保存列表
        //批量保存
        TODO()
    }

    /**
     * 从数据库查找指定id的animation的拓扑关系。
     */
    private fun find(animationId: Int): AnimationModel? {
        return database.from(Animations).select(Animations.relations, Animations.relationsTopology)
                .where { Animations.id eq animationId }
                .firstOrNull()
                ?.let { AnimationModel(animationId, it[Animations.relations]!!, it[Animations.relationsTopology]!!) }
    }

    /**
     * 从数据库查找全部id的animation的拓扑关系。
     */
    private fun findAll(animationIds: Collection<Int>): List<AnimationModel> {
        return database.from(Animations).select(Animations.id, Animations.relations, Animations.relationsTopology)
                .where { Animations.id inList animationIds }
                .map { AnimationModel(it[Animations.id]!!, it[Animations.relations]!!, it[Animations.relationsTopology]!!) }
    }

    class AnimationModel(val id: Int, val relations: Map<RelationType, List<Int>>, val relationsTopology: Map<RelationType, List<Int>>) {
        override fun equals(other: Any?): Boolean {
            return this === other || (other is AnimationModel && this.id == other.id)
        }

        override fun hashCode() = id
    }

    /**
     * 比较两个关系拓扑。找出新拓扑中比旧拓扑多出的新节点。
     */
    private fun compareRelationAdds(oldRelations: Map<RelationType, List<Int>>, newRelations: Map<RelationType, List<Int>>): Set<Int> {
        val old = oldRelations.flatMap { (_, v) -> v }.toHashSet()
        val new = newRelations.flatMap { (_, v) -> v }.toHashSet()

        return new - old
    }

    /**
     * 比较两个关系拓扑。查看两个拓扑是否等价。
     * 不能通过#compareRelationAdds集合为空来做这项判断，因为有可能变换关系类型。
     */
    private fun compareRelationEquals(oldRelations: Map<RelationType, List<Int>>, newRelations: Map<RelationType, List<Int>>): Boolean {
        val keys = oldRelations.keys + newRelations.keys
        for (key in keys) {
            val old = oldRelations[key]?.toHashSet() ?: emptySet()
            val new = newRelations[key]?.toHashSet() ?: emptySet()
            if(old != new) {
                return false
            }
        }
        return true
    }
}