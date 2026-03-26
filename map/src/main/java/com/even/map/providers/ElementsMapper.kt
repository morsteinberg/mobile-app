package com.even.map.providers

import com.even.map.types.Element

class ElementsMapper {
    private val elementIdsToElementsMap = mutableMapOf<String, Element>()
    private val externalIdsToElementIdsMap = mutableMapOf<String, String>()
    private val elementIdsToExternalIdsMap = mutableMapOf<String, MutableSet<String>>()

    fun getElementIdByExternalId(externalId: String): String? =
        externalIdsToElementIdsMap[externalId]

    fun getExternalIdsByElementId(elementId: String): Set<String> =
        elementIdsToExternalIdsMap[elementId] ?: emptySet()

    fun getExternalIdsByElement(element: Element): Set<String> =
        getExternalIdsByElementId(element.id)

    fun put(element: Element, vararg externalIds: String) {
        elementIdsToElementsMap[element.id] = element
        externalIds.forEach { externalId -> externalIdsToElementIdsMap[externalId] = element.id }
        elementIdsToExternalIdsMap.getOrPut(element.id) { mutableSetOf() }.addAll(externalIds)
    }

    fun remove(elementId: String) {
        elementIdsToElementsMap.remove(elementId)
        elementIdsToExternalIdsMap[elementId]?.forEach { externalId ->
            externalIdsToElementIdsMap.remove(externalId)
        }
        elementIdsToExternalIdsMap.remove(elementId)
    }
}
