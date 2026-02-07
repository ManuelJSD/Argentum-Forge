**Mapas V1 [0.99z/0.11.2]**

Carga de mapas V1:
```
Public Sub MapaV1_Cargar(ByVal Map As String, ByRef buffer() As MapBlock, ByVal SoloMap As Boolean)
'*************************************************
'Author: ^[GS]^
'Last modified: 20/05/06
'*************************************************

    On Error Resume Next
    Dim TBlock As Byte
    Dim loopc As Integer
    Dim TempInt As Integer
    Dim Body As Integer
    Dim Head As Integer
    Dim Heading As Byte
    Dim Y As Integer
    Dim X As Integer
    Dim FreeFileMap As Long
    Dim FreeFileInf As Long
    
    'Change mouse icon
    frmMain.MousePointer = 11
    
    'Open files
    FreeFileMap = FreeFile
    Open Map For Binary As FreeFileMap
    
    
    Seek FreeFileMap, 1
    
    If Not SoloMap Then
        Map = Left$(Map, Len(Map) - 4)
        Map = Map & ".inf"
        FreeFileInf = FreeFile
        Open Map For Binary As #2
        Seek FreeFileInf, 1
    End If
    
    'Cabecera map
    Get FreeFileMap, , MapInfo.MapVersion
    Get FreeFileMap, , MiCabecera
    Get FreeFileMap, , TempInt
    Get FreeFileMap, , TempInt
    Get FreeFileMap, , TempInt
    Get FreeFileMap, , TempInt
    
    If Not SoloMap Then
        'Cabecera inf
        Get FreeFileInf, , TempInt
        Get FreeFileInf, , TempInt
        Get FreeFileInf, , TempInt
        Get FreeFileInf, , TempInt
        Get FreeFileInf, , TempInt
    End If
    
    'Load arrays
    For Y = YMinMapSize To YMaxMapSize
        For X = XMinMapSize To XMaxMapSize
    
            '.map file
            Get FreeFileMap, , buffer(X, Y).Blocked
            
            For loopc = 1 To 4
                Get FreeFileMap, , buffer(X, Y).Graphic(loopc).grhIndex
                'Set up GRH
                If buffer(X, Y).Graphic(loopc).grhIndex > 0 Then
                    InitGrh buffer(X, Y).Graphic(loopc), buffer(X, Y).Graphic(loopc).grhIndex
                End If
            Next loopc
            'Trigger
            Get FreeFileMap, , buffer(X, Y).Trigger
            
            Get FreeFileMap, , TempInt
            
            If Not SoloMap Then
                '.inf file
                
                'Tile exit
                Get FreeFileInf, , buffer(X, Y).TileExit.Map
                Get FreeFileInf, , buffer(X, Y).TileExit.X
                Get FreeFileInf, , buffer(X, Y).TileExit.Y
                              
                'make NPC
                Get FreeFileInf, , buffer(X, Y).NPCIndex
                If buffer(X, Y).NPCIndex > 0 Then
                    Body = NpcData(buffer(X, Y).NPCIndex).Body
                    Head = NpcData(buffer(X, Y).NPCIndex).Head
                    Heading = NpcData(buffer(X, Y).NPCIndex).Heading
                    Call MakeChar(NextOpenChar(), Body, Head, Heading, X, Y)
                End If
                
                'Make obj
                Get FreeFileInf, , buffer(X, Y).OBJInfo.objindex
                Get FreeFileInf, , buffer(X, Y).OBJInfo.Amount
                If buffer(X, Y).OBJInfo.objindex > 0 Then
                    InitGrh buffer(X, Y).ObjGrh, ObjData(buffer(X, Y).OBJInfo.objindex).grhIndex
                End If
                
                'Empty place holders for future expansion
                Get FreeFileInf, , TempInt
                Get FreeFileInf, , TempInt
            End If
        Next X
    Next Y
    
    'Close files
    Close FreeFileMap
    
    If Not SoloMap Then
        Close FreeFileInf
        
        Call Pestañas(Map)
        
        bRefreshRadar = True ' Radar
        
        Map = Left$(Map, Len(Map) - 4) & ".dat"
            
        MapInfo_Cargar Map
        frmMain.lblMapVersion.Caption = MapInfo.MapVersion
        
        'Set changed flag
        MapInfo.Changed = 0
        
        ' Vacia el Deshacer
        modEdicion.Deshacer_Clear
    End If
    
    'Change mouse icon
    frmMain.MousePointer = 0
    MapaCargado = True
End Sub
```

Guardado de mapas V1:
```
Public Sub MapaV1_Guardar(SaveAs As String)
'*************************************************
'Author: ^[GS]^
'Last modified: 20/05/06
'*************************************************

On Error GoTo ErrorSave
    Dim FreeFileMap As Long
    Dim FreeFileInf As Long
    Dim loopc As Long
    Dim TempInt As Integer
    Dim Y As Long
    Dim X As Long
    
    If FileExist(SaveAs, vbNormal) = True Then
        If MsgBox("¿Desea sobrescribir " & SaveAs & "?", vbCritical + vbYesNo) = vbNo Then
            Exit Sub
        Else
            Kill SaveAs
        End If
    End If
    
    'Change mouse icon
    frmMain.MousePointer = 11
    
    If FileExist(Left$(SaveAs, Len(SaveAs) - 4) & ".inf", vbNormal) = True Then
        Kill Left$(SaveAs, Len(SaveAs) - 4) & ".inf"
    End If
    
    'Open .map file
    FreeFileMap = FreeFile
    Open SaveAs For Binary As FreeFileMap
    Seek FreeFileMap, 1
    
    
    SaveAs = Left$(SaveAs, Len(SaveAs) - 4)
    SaveAs = SaveAs & ".inf"
    'Open .inf file
    FreeFileInf = FreeFile
    Open SaveAs For Binary As FreeFileInf
    Seek FreeFileInf, 1
    'map Header
    If frmMain.lblMapVersion.Caption < 32767 Then
        frmMain.lblMapVersion.Caption = frmMain.lblMapVersion + 1
        frmMapInfo.txtMapVersion = frmMain.lblMapVersion.Caption
    End If
    Put FreeFileMap, , CInt(frmMain.lblMapVersion.Caption)
    Put FreeFileMap, , MiCabecera
    
    Put FreeFileMap, , TempInt
    Put FreeFileMap, , TempInt
    Put FreeFileMap, , TempInt
    Put FreeFileMap, , TempInt
    
    'inf Header
    Put FreeFileInf, , TempInt
    Put FreeFileInf, , TempInt
    Put FreeFileInf, , TempInt
    Put FreeFileInf, , TempInt
    Put FreeFileInf, , TempInt
    
    'Write .map file
    For Y = YMinMapSize To YMaxMapSize
        For X = XMinMapSize To XMaxMapSize
            
            '.map file
            
            ' Bloqueos
            Put FreeFileMap, , MapData(X, Y).Blocked
            
            ' Capas
            For loopc = 1 To 4
                If loopc = 2 Then Call FixCoasts(MapData(X, Y).Graphic(loopc).grhIndex, X, Y)
                Put FreeFileMap, , MapData(X, Y).Graphic(loopc).grhIndex
            Next loopc
            
            ' Triggers
            Put FreeFileMap, , MapData(X, Y).Trigger
            Put FreeFileMap, , TempInt
            
            '.inf file
            'Tile exit
            Put FreeFileInf, , MapData(X, Y).TileExit.Map
            Put FreeFileInf, , MapData(X, Y).TileExit.X
            Put FreeFileInf, , MapData(X, Y).TileExit.Y
            
            'NPC
            Put FreeFileInf, , MapData(X, Y).NPCIndex
            
            'Object
            Put FreeFileInf, , MapData(X, Y).OBJInfo.objindex
            Put FreeFileInf, , MapData(X, Y).OBJInfo.Amount
            
            'Empty place holders for future expansion
            Put FreeFileInf, , TempInt
            Put FreeFileInf, , TempInt
            
        Next X
    Next Y
    
    'Close .map file
    Close FreeFileMap
    'Close .inf file
    Close FreeFileInf
    
    Call Pestañas(SaveAs)
    
    'write .dat file
    SaveAs = Left$(SaveAs, Len(SaveAs) - 4) & ".dat"
    MapInfo_Guardar SaveAs
    
    'Change mouse icon
    frmMain.MousePointer = 0
    MapInfo.Changed = 0
    
Exit Sub
ErrorSave:
    MsgBox "Error " & Err.Number & " - " & Err.Description
End Sub
```

**Mapas V1 [0.11.5/0.12/0.13/AOLibre]**

Carga de mapas V2:
```
Public Sub MapaV2_Cargar(ByVal Map As String, ByRef buffer() As MapBlock, ByVal SoloMap As Boolean)
'*************************************************
'Author: ^[GS]^
'Last modified: 20/05/06
'*************************************************

On Error Resume Next
    Dim loopc As Integer
    Dim TempInt As Integer
    Dim Body As Integer
    Dim Head As Integer
    Dim Heading As Byte
    Dim Y As Integer
    Dim X As Integer
    Dim ByFlags As Byte
    Dim FreeFileMap As Long
    Dim FreeFileInf As Long
    
    'Change mouse icon
    frmMain.MousePointer = 11
       
    'Open files
    FreeFileMap = FreeFile
    Open Map For Binary As FreeFileMap
    Seek FreeFileMap, 1
    
    If Not SoloMap Then
        Map = Left$(Map, Len(Map) - 4)
        Map = Map & ".inf"
        
        FreeFileInf = FreeFile
        Open Map For Binary As FreeFileInf
        Seek FreeFileInf, 1
    End If
    
    'Cabecera map
    Get FreeFileMap, , MapInfo.MapVersion
    Get FreeFileMap, , MiCabecera
    Get FreeFileMap, , TempInt
    Get FreeFileMap, , TempInt
    Get FreeFileMap, , TempInt
    Get FreeFileMap, , TempInt
    
    If Not SoloMap Then
        'Cabecera inf
        Get FreeFileInf, , TempInt
        Get FreeFileInf, , TempInt
        Get FreeFileInf, , TempInt
        Get FreeFileInf, , TempInt
        Get FreeFileInf, , TempInt
    End If
    
    'Load arrays
    For Y = YMinMapSize To YMaxMapSize
        For X = XMinMapSize To XMaxMapSize
    
            Get FreeFileMap, , ByFlags
            
            buffer(X, Y).Blocked = (ByFlags And 1)
            
            Get FreeFileMap, , buffer(X, Y).Graphic(1).grhIndex
            InitGrh buffer(X, Y).Graphic(1), buffer(X, Y).Graphic(1).grhIndex
            
            'Layer 2 used?
            If ByFlags And 2 Then
                Get FreeFileMap, , buffer(X, Y).Graphic(2).grhIndex
                InitGrh buffer(X, Y).Graphic(2), buffer(X, Y).Graphic(2).grhIndex
            Else
                buffer(X, Y).Graphic(2).grhIndex = 0
            End If
                
            'Layer 3 used?
            If ByFlags And 4 Then
                Get FreeFileMap, , buffer(X, Y).Graphic(3).grhIndex
                InitGrh buffer(X, Y).Graphic(3), buffer(X, Y).Graphic(3).grhIndex
            Else
                buffer(X, Y).Graphic(3).grhIndex = 0
            End If
                
            'Layer 4 used?
            If ByFlags And 8 Then
                Get FreeFileMap, , buffer(X, Y).Graphic(4).grhIndex
                InitGrh buffer(X, Y).Graphic(4), buffer(X, Y).Graphic(4).grhIndex
            Else
                buffer(X, Y).Graphic(4).grhIndex = 0
            End If
            
             
            'Trigger used?
            If ByFlags And 16 Then
                Get FreeFileMap, , buffer(X, Y).Trigger
            Else
                buffer(X, Y).Trigger = 0
            End If
            
            If Not SoloMap Then
                '.inf file
                Get FreeFileInf, , ByFlags
                
                If ByFlags And 1 Then
                    Get FreeFileInf, , buffer(X, Y).TileExit.Map
                    Get FreeFileInf, , buffer(X, Y).TileExit.X
                    Get FreeFileInf, , buffer(X, Y).TileExit.Y
                End If
        
                If ByFlags And 2 Then
                    'Get and make NPC
                    Get FreeFileInf, , buffer(X, Y).NPCIndex
        
                    If buffer(X, Y).NPCIndex < 0 Then
                        buffer(X, Y).NPCIndex = 0
                    Else
                        Body = NpcData(buffer(X, Y).NPCIndex).Body
                        Head = NpcData(buffer(X, Y).NPCIndex).Head
                        Heading = NpcData(buffer(X, Y).NPCIndex).Heading
                        Call MakeChar(NextOpenChar(), Body, Head, Heading, X, Y)
                    End If
                End If
        
                If ByFlags And 4 Then
                    'Get and make Object
                    Get FreeFileInf, , buffer(X, Y).OBJInfo.objindex
                    Get FreeFileInf, , buffer(X, Y).OBJInfo.Amount
                    If buffer(X, Y).OBJInfo.objindex > 0 Then
                        InitGrh buffer(X, Y).ObjGrh, ObjData(buffer(X, Y).OBJInfo.objindex).grhIndex
                    End If
                End If
            End If
        Next X
    Next Y
    
    'Close files
    Close FreeFileMap
    
    If Not SoloMap Then
        Close FreeFileInf
        
        Call Pestañas(Map)
        
        bRefreshRadar = True ' Radar
        
        Map = Left$(Map, Len(Map) - 4) & ".dat"
        
        MapInfo_Cargar Map
        frmMain.lblMapVersion.Caption = MapInfo.MapVersion
        
        'Set changed flag
        MapInfo.Changed = 0
        
        ' Vacia el Deshacer
        modEdicion.Deshacer_Clear
    End If
    
    'Change mouse icon
    frmMain.MousePointer = 0
    MapaCargado = True
End Sub
```

Guardado de mapas V2:
```
Public Sub MapaV2_Guardar(ByVal SaveAs As String)
'*************************************************
'Author: ^[GS]^
'Last modified: 20/05/06
'*************************************************

On Error GoTo ErrorSave
Dim FreeFileMap As Long
Dim FreeFileInf As Long
Dim loopc As Long
Dim TempInt As Integer
Dim Y As Long
Dim X As Long
Dim ByFlags As Byte

If FileExist(SaveAs, vbNormal) = True Then
    If MsgBox("¿Desea sobrescribir " & SaveAs & "?", vbCritical + vbYesNo) = vbNo Then
        Exit Sub
    Else
        Kill SaveAs
    End If
End If

frmMain.MousePointer = 11

' y borramos el .inf tambien
If FileExist(Left$(SaveAs, Len(SaveAs) - 4) & ".inf", vbNormal) = True Then
    Kill Left$(SaveAs, Len(SaveAs) - 4) & ".inf"
End If

'Open .map file
FreeFileMap = FreeFile
Open SaveAs For Binary As FreeFileMap
Seek FreeFileMap, 1

SaveAs = Left$(SaveAs, Len(SaveAs) - 4)
SaveAs = SaveAs & ".inf"

'Open .inf file
FreeFileInf = FreeFile
Open SaveAs For Binary As FreeFileInf
Seek FreeFileInf, 1

    'map Header
    
    ' Version del Mapa
    If frmMain.lblMapVersion.Caption < 32767 Then
        frmMain.lblMapVersion.Caption = frmMain.lblMapVersion + 1
        frmMapInfo.txtMapVersion = frmMain.lblMapVersion.Caption
    End If
    Put FreeFileMap, , CInt(frmMain.lblMapVersion.Caption)
    Put FreeFileMap, , MiCabecera
    Put FreeFileMap, , TempInt
    Put FreeFileMap, , TempInt
    Put FreeFileMap, , TempInt
    Put FreeFileMap, , TempInt
    
    'inf Header
    Put FreeFileInf, , TempInt
    Put FreeFileInf, , TempInt
    Put FreeFileInf, , TempInt
    Put FreeFileInf, , TempInt
    Put FreeFileInf, , TempInt
    
    'Write .map file
    For Y = YMinMapSize To YMaxMapSize
        For X = XMinMapSize To XMaxMapSize
            
                ByFlags = 0
                
                If MapData(X, Y).Blocked = 1 Then ByFlags = ByFlags Or 1
                If MapData(X, Y).Graphic(2).grhIndex Then ByFlags = ByFlags Or 2
                If MapData(X, Y).Graphic(3).grhIndex Then ByFlags = ByFlags Or 4
                If MapData(X, Y).Graphic(4).grhIndex Then ByFlags = ByFlags Or 8
                If MapData(X, Y).Trigger Then ByFlags = ByFlags Or 16
                
                Put FreeFileMap, , ByFlags
                
                Put FreeFileMap, , MapData(X, Y).Graphic(1).grhIndex
                
                For loopc = 2 To 4
                    If MapData(X, Y).Graphic(loopc).grhIndex Then _
                        Put FreeFileMap, , MapData(X, Y).Graphic(loopc).grhIndex
                Next loopc
                
                If MapData(X, Y).Trigger Then _
                    Put FreeFileMap, , MapData(X, Y).Trigger
                
                '.inf file
                
                ByFlags = 0
                
                If MapData(X, Y).TileExit.Map Then ByFlags = ByFlags Or 1
                If MapData(X, Y).NPCIndex Then ByFlags = ByFlags Or 2
                If MapData(X, Y).OBJInfo.objindex Then ByFlags = ByFlags Or 4
                
                Put FreeFileInf, , ByFlags
                
                If MapData(X, Y).TileExit.Map Then
                    Put FreeFileInf, , MapData(X, Y).TileExit.Map
                    Put FreeFileInf, , MapData(X, Y).TileExit.X
                    Put FreeFileInf, , MapData(X, Y).TileExit.Y
                End If
                
                If MapData(X, Y).NPCIndex Then
                
                    Put FreeFileInf, , CInt(MapData(X, Y).NPCIndex)
                End If
                
                If MapData(X, Y).OBJInfo.objindex Then
                    Put FreeFileInf, , MapData(X, Y).OBJInfo.objindex
                    Put FreeFileInf, , MapData(X, Y).OBJInfo.Amount
                End If
            
        Next X
    Next Y
    
    'Close .map file
    Close FreeFileMap
    
    'Close .inf file
    Close FreeFileInf


Call Pestañas(SaveAs)

'write .dat file
SaveAs = Left$(SaveAs, Len(SaveAs) - 4) & ".dat"
MapInfo_Guardar SaveAs

'Change mouse icon
frmMain.MousePointer = 0
MapInfo.Changed = 0

Exit Sub

ErrorSave:
    MsgBox "Error en GuardarV2, nro. " & Err.Number & " - " & Err.Description
End Sub
```