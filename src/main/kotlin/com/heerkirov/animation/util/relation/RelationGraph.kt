package com.heerkirov.animation.util.relation

import java.lang.RuntimeException
import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class RelationGraph<E: Any, R: IRelation<R>>(private val elements: Array<E>, initializer: RelationGraph<E, R>.Builder.() -> Unit) {
    //以二维数组表示的，index为[from][to]的entity之间的关系
    private val map: ArrayList<ArrayList<R?>> = ArrayList(IntRange(0, elements.size).map {
        ArrayList(IntRange(0, elements.size).map {
            null
        })
    })

    //将entity的hashCode映射到其index
    private val hashMapper: HashMap<Int, Int> = HashMap(elements.size)

    init {
        //初始化时对全部entity进行扫描，确认无重复，并将hashCode映射存入map
        elements.forEachIndexed { index, element ->
            val hashCode = element.hashCode()
            if(hashMapper.containsKey(hashCode)) {
                throw RuntimeException("Element[$index] is duplicated.")
            }
            hashMapper[hashCode] = index
        }
        //执行关系初始化代码块
        initializer(Builder())
        //执行关系传播代码块
        map.indices.forEach { spread(it) }
    }

    inner class Builder {
        /**
         * 在图中添加两个节点之间的有向关系。
         */
        fun addRelation(from: E, relation: R, to: E) {
            val fromIndex = hashMapper[from.hashCode()] ?: throw NoSuchElementException("From element is not in graph.")
            val toIndex = hashMapper[to.hashCode()] ?: throw NoSuchElementException("To element is not in graph.")
            map[fromIndex][toIndex] = relation
        }
    }

    /**
     * 将当前的有限的拓扑图中的关系传播到全部联通节点，使任意两个联通节点之间的关系可直接查询。
     */
    private fun spread(thisIndex: Int) {
        //记录传播过程中已经遍历过的节点，防止重复遍历
        val been = HashSet<Int>()
        //BFS的队列
        val queue = LinkedList<Int>()
        //将初始节点加入
        been.add(thisIndex)
        queue.add(thisIndex)

        while(queue.isNotEmpty()) {
            val currentIndex = queue.pop()
            val relationOfThisToCurrent = map[thisIndex][currentIndex]!!
            val mapper = map[currentIndex]
            for(goalIndex in mapper.indices) {
                val relationOfCurrentToGoal = mapper[goalIndex]
                if(relationOfCurrentToGoal != null) {
                    //根据this->current和current->goal的关系，计算this->goal的关系
                    val relationOfThisToGoal = if(thisIndex == currentIndex) { relationOfCurrentToGoal }else{
                        relationOfThisToCurrent.spread(relationOfCurrentToGoal)
                    }
                    //在目标节点没有遍历过，或者在关系更新过的情况下将goal节点放入队列
                    if(putNewRelation(thisIndex, goalIndex, relationOfThisToGoal) || !been.contains(goalIndex)) {
                        queue.add(goalIndex)
                        been.add(goalIndex)
                    }
                }
            }
        }
    }

    /**
     * 按照规约在关系表中更新关系。只有新关系大于旧关系时才会正确放入，并返回True。
     */
    private fun putNewRelation(from: Int, to: Int, newRelation: R): Boolean {
        return if (from != to) {
            val oldRelation = map[from][to]
            if(oldRelation == null || oldRelation < newRelation) {
                map[from][to] = newRelation
                map[to][from] = newRelation.reverse()
                true
            }else{
                false
            }
        } else {
            false
        }
    }
}