package com.even.map.terraexplorer.constants

internal sealed class Property<T>(val key: String) {
    object BackgroundColor : Property<String>("Background Color")
    object Bold : Property<Boolean>("Bold")
    object Font : Property<String>("Font")
    object ImageMaxSize : Property<Double>("Image Max Size")
    object ImageFile : Property<String>("Image file")
    object ImageOpacity : Property<Double>("Image Opacity")
    object PivotAlignment : Property<Int>("Pivot Alignment")
    object Scale : Property<Int>("Scale")
    object SmallestVisibleSize : Property<Int>("Smallest Visible Size")
    object Text : Property<String>("Text")
    object TextSize : Property<Int>("Text Size")
}
