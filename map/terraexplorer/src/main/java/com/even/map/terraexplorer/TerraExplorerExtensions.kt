package com.even.map.terraexplorer

import com.even.map.terraexplorer.constants.Attribute
import com.even.map.terraexplorer.constants.Property
import com.even.map.types.Location
import com.skyline.teapi81.IFeatureGroup
import com.skyline.teapi81.IFeatureLayer
import com.skyline.teapi81.IPosition
import com.skyline.teapi81.ITerrainRasterLayer

internal fun <T> IFeatureGroup.setProperty(property: Property<T>, value: T) {
    SetProperty(property.key, value)
}

internal fun <T> IFeatureGroup.setPropertyByFeatureAttribute(property: Property<T>, attribute: Attribute) {
    SetClassification(property.key, attribute.toClassification())
}

internal val IFeatureLayer.attributes
    get() = this.dataSourceInfo.attributes

internal fun IFeatureLayer.updateVisibility(isVisible: Boolean) {
    if (visibility.show != isVisible) visibility.show = isVisible
}

internal fun IPosition.toLocation() = Location(latitude = this.y, longitude = this.x, altitude = this.altitude)

internal fun ITerrainRasterLayer.updateVisibility(isVisible: Boolean) {
    if (visibility.show != isVisible) visibility.show = isVisible
}
