package com.heerkirov.animation.service.manager

import com.heerkirov.animation.dao.Animations
import com.heerkirov.animation.enums.ErrCode
import com.heerkirov.animation.enums.RelationType
import com.heerkirov.animation.exception.BadRequestException
import com.heerkirov.animation.util.relation.RelationGraph
import com.heerkirov.animation.util.ktorm.dsl.*
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AnimationRelationProcessor(@Autowired private val database: Database) {
    /**
     * 对单个animation的relation进行更新。
     * 首先根据旧的全量拓扑，找出所有关联对象。然后加入新的拓扑关联对象，构成全量图。
     * 然后，对全量图进行关系传播推导，导出所有对象的全量拓扑，并更新那些拓扑发生变化的对象。
     * @throws NoSuchElementException 找不到指定id的animation。
     */
    fun updateRelationTopology(animationId: Int, relations: Map<RelationType, List<Int>>) {
        val newRelations = validateRelation(relations)
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
        val graph = RelationGraph<AnimationModel, RelationType>(elements.values.sortedBy { it.createTime }.toTypedArray()) {
            for (animation in elements.values) {
                val r = if(animation == thisAnimation) { newRelations }else{ animation.relations }
                for ((relation, list) in r.entries) {
                    for (i in list) {
                        addRelation(animation, relation, elements[i]!!)
                    }
                }
            }
        }

        //从传播图导出每一个节点的全量拓扑
        //比对每个节点的新旧全量拓扑，发生变化的放入保存列表；主对象要更新关联拓扑，也要放入保存列表
        //批量保存
        database.batchUpdate(Animations) {
            for (element in elements.values) {
                if(element != thisAnimation) {
                    val newTopology = graph[element].map { (k, v) -> Pair(k, v.map { it.id }) }.toMap()
                    if(!compareRelationEquals(element.relationsTopology, newTopology)) {
                        item {
                            set(it.relationsTopology, newTopology)
                            where { it.id eq element.id }
                        }
                    }
                }
            }
        }
        val newThisTopology = graph[thisAnimation].map { (k, v) -> Pair(k, v.map { it.id }) }.toMap()
        database.update(Animations) {
            set(it.relations, newRelations)
            set(it.relationsTopology, newThisTopology)
            where { it.id eq thisAnimation.id }
        }
    }

    /**
     * 对全部animation的relation进行更新。
     * @return 有多少animation得到了更新
     */
    fun updateAllRelationTopology(): Int {
        val elements = database.from(Animations)
                .select(Animations.id, Animations.relations, Animations.relationsTopology, Animations.createTime)
                .orderBy(Animations.createTime.asc())
                .map { AnimationModel(it[Animations.id]!!, it[Animations.relations]!!, it[Animations.relationsTopology]!!, it[Animations.createTime]!!) }

        val maps = elements.map { Pair(it.id, it) }.toMap()

        val graph = RelationGraph<AnimationModel, RelationType>(elements.toTypedArray()) {
            for (element in elements) {
                for ((r, list) in element.relations.entries) {
                    for (i in list) {
                        addRelation(element, r, maps[i] ?: throw NoSuchElementException("Cannot find animation $i."))
                    }
                }
            }
        }

        var num = 0
        database.batchUpdate(Animations) {
            for (element in elements) {
                val topology = graph[element].map { (k, v) -> Pair(k, v.map { it.id }) }.toMap()
                if(topology.isNotEmpty()) {
                    item {
                        where { it.id eq element.id }
                        set(it.relationsTopology, topology)
                    }
                    num += 1
                }
            }
        }
        return num
    }

    /**
     * 从一个animation的全部关联中移除此animation。
     * 首先根据全量拓扑，找出所有关联对象。将除原对象外的所有关联对象放入图。
     * 遍历这些对象的所有直接关联关系，从之中移除全部原对象，然后将剩余关系放入图。
     * 随后推导全量图，将新的全量拓扑和变化的关联对象更新到其对应的对象。
     */
    fun removeAnimationInTopology(animationId: Int, topology: Map<RelationType, List<Int>>) {
        //节点列表
        val elements = hashMapOf<Int, AnimationModel>()
        //查找全量拓扑的关联节点，放入图中
        elements.putAll(findAll(topology.flatMap { (_, v) -> v }).map { Pair(it.id, it) })
        //relation发生变动的节点
        val relationChanges = hashMapOf<Int, Map<RelationType, List<Int>>>()
        //构建图
        val graph = RelationGraph<AnimationModel, RelationType>(elements.values.sortedBy { it.createTime }.toTypedArray()) {
            for (animation in elements.values) {
                val newRelations = findAndRemoveIdInRelation(animation.relations, animationId)
                for((r, list) in (newRelations ?: animation.relations).entries) {
                    for(i in list) {
                        addRelation(animation, r, elements[i]!!)
                    }
                }
                if(newRelations != null) {
                    relationChanges[animation.id] = newRelations
                }
            }
        }

        //从传播图导出每一个节点的全量拓扑
        //比对每个节点的全量拓扑，发生变化的放入保存列表。relation发生变动的，也要放入保存列表
        //批量保存
        database.batchUpdate(Animations) {
            for(element in elements.values) {
                val newRelations = relationChanges[element.id]
                val newTopology = graph[element].map { (k, v) -> Pair(k, v.map { it.id }) }.toMap()
                if(newRelations != null || !compareRelationEquals(element.relationsTopology, newTopology)) {
                    item {
                        where { it.id eq element.id }
                        set(it.relations, newRelations ?: element.relations)
                        set(it.relationsTopology, newTopology)
                    }
                }
            }
        }
    }

    /**
     * 从数据库查找指定id的animation的拓扑关系。
     */
    private fun find(animationId: Int): AnimationModel? {
        return database.from(Animations).select(Animations.relations, Animations.relationsTopology, Animations.createTime)
                .where { Animations.id eq animationId }
                .firstOrNull()
                ?.let { AnimationModel(animationId, it[Animations.relations]!!, it[Animations.relationsTopology]!!, it[Animations.createTime]!!) }
    }

    /**
     * 从数据库查找全部id的animation的拓扑关系。
     */
    private fun findAll(animationIds: Collection<Int>): List<AnimationModel> {
        if(animationIds.isEmpty()) {
            return emptyList()
        }
        val rowSet = database.from(Animations).select(Animations.id, Animations.relations, Animations.relationsTopology, Animations.createTime)
                .where { Animations.id inList animationIds }
                .limit(0, animationIds.size)
        if(rowSet.totalRecords < animationIds.size) {
            val minus = animationIds.toSet() - rowSet.map { it[Animations.id]!! }.toSet()
            throw BadRequestException(ErrCode.NOT_EXISTS, "Animation ${minus.joinToString(", ")} not exists.")
        }else{
            return rowSet.map { AnimationModel(it[Animations.id]!!, it[Animations.relations]!!, it[Animations.relationsTopology]!!, it[Animations.createTime]!!) }
        }
    }

    class AnimationModel(val id: Int,
                         val relations: Map<RelationType, List<Int>>,
                         val relationsTopology: Map<RelationType, List<Int>>,
                         val createTime: LocalDateTime) {
        override fun equals(other: Any?): Boolean {
            return this === other || (other is AnimationModel && this.id == other.id)
        }

        override fun hashCode() = id
    }

    /**
     * 对目标拓扑进行优化整理。
     * - 去除没有项的关系。
     * - 移除同关系下重复的id。
     * - 如果存在不同关系下重复的id，那么抛出异常。
     */
    private fun validateRelation(relations: Map<RelationType, List<Int>>): Map<RelationType, List<Int>> {
        val map = HashMap<RelationType, List<Int>>()
        val intSet = HashSet<Int>()

        for ((r, list) in relations.entries) {
            if(list.isNotEmpty()) {
                val distinctList = list.distinct()
                for (i in distinctList) {
                    if(intSet.contains(i)) {
                        throw BadRequestException(ErrCode.PARAM_ERROR, "Relation of animation $i is duplicated.")
                    }
                }
                intSet.addAll(distinctList)
                map[r] = distinctList
            }
        }

        return map
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
            val old = oldRelations[key]?.toHashSet() ?: hashSetOf()
            val new = newRelations[key]?.toHashSet() ?: hashSetOf()
            if(old != new) {
                return false
            }
        }
        return true
    }

    /**
     * 在关系中查找并移除指定的id。如果至少有一个id被移除，那么返回变更后的关系。
     */
    private fun findAndRemoveIdInRelation(relations: Map<RelationType, List<Int>>, id: Int): Map<RelationType, List<Int>>? {
        val map = HashMap<RelationType, List<Int>>()
        var any = false
        for ((r, list) in relations.entries) {
            val newList = list.filter { it != id }
            if(newList.isNotEmpty()) map[r] = newList
            if(newList.size < list.size) any = true
        }
        return if(any) map else null
    }
}