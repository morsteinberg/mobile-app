package com.even.map.terraexplorer.constants

import com.even.core.extensions.StringExtensions.withFileExtension
import com.even.core.types.FileType

internal object LayerInitParam {
    fun ofFeatureLayer(fileName: String) = "FileName=${fileName.withFileExtension(FileType.GEOPACKAGE)};TEPlugName=OGR;LayerName=$fileName"

    fun ofMTPElevaitonLayer(wmtsHostName: String, remoteFileName: String) =
        "<EXT><ExtInfo><![CDATA[<Skyline version =\"1.0.0\"><ServerList name=\"$remoteFileName\"><Server priority=\"1\">$wmtsHostName$/sg/default</Server></ServerList></Skyline>]]></ExtInfo><ExtType>mpt</ExtType></EXT>"

    fun ofRasterLayer(wmtsHostName: String, remoteFileName: String) =
        "<EXT><ExtInfo><![CDATA[[INFO]\r\nProtocolType=3\r\nMeters=0\r\nMPP=5.36441802978515625e-006\r\nUrl=https://$wmtsHostName/service/?request=GetTile&Version=1.0.0&Service=WMTS&Layer=$remoteFileName&Style=default&Format=image/png&TileMatrixSet=epsg4326grid&TileMatrix=00&TileRow=0&TileCol=0\r\nLayers=$remoteFileName\r\nCapabilitiesUrl=https://$wmtsHostName/service/?\r\nxul=-180\r\nylr=-90\r\nxlr=180\r\nyul=90\r\nWMSCoordSys=EPSG:4326\r\nWKT=GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9122\"]],AXIS[\"Latitude\",NORTH],AXIS[\"Longitude\",EAST],AUTHORITY[\"EPSG\",\"4326\"]]\r\nTileInfo=0000039EEzrv.vR......A.A......A.....................2.l.....f...EV.A....4..lD#nC.....A.A......C..........A..........2.l.....f...EV.A....4..VD#qC.....A.A......G..........C..........2.l.....f...EV.A....4..FD#tC.....A.A......O..........G..........2.l.....f...EV.A....n..2D#wC.....A.A......e..........O..........2.l.....f...EV.A....n..lD#zC.....A.A......#..........e..........2.l.....f...EV.A....n..VD#3C.....A.A....D.#..........#..........2.l.....f...EV.A....n..FD#6C.....A.A....L.#.........A#..........2.l.....f...EV.A....X..2D#9C.....A.A....L.#A........C#..........2.l.....f...EV.A....X..lH#.C.....A.A....L.#C........C#A.........2.l.....f...EV.A....X..VH#CC.....A.A....L.#G........C#C.........2.l.....f...EV.A....X..FH#FC.....A.A....L.#O........C#G.........2.l.....f...EV.A....H..2H#IC.....A.A....L.#e........C#O.........2.l.....f...EV.A....H..lH#LC.....A.A....L.##........C#e.........2.l.....f...EV.A....H..VH#OC.....A.A....b.##........C##.........2.l.....f...EV.A....H..FH#RC.....A.A....8.##........G##.........2.l.....f...EV.A....4..2H0UC.....A.A....8.##.A......O##.........2.l.....f...EV.A....4..lH0XC.....A.A....8.##.C......O##A........2.l.....f...EV.A....4..VH0aC...v.v.v.w.v.x.v.y.v.z.v.1.v.2.v.3.v.4.v.5.w.v.w.w.w.x.w.y.w.z.w.1.w.2.w.3..\r\n]]></ExtInfo><ExtType>wms</ExtType></EXT>"
}

internal enum class LayerPlugName(val value: String) {
    GIS("gisplg.rct"),
    MPT("mptplg.rct");
}
