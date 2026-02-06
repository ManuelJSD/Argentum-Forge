A continuación se muestra el código de la función LoadGrhData que se encarga de cargar los datos de los gráficos en el juego. Hay varias versiones de esta función, dependiendo de si se usa cabecera o no, o si se usa Integer o Long.

**LoadGrhData Integer Version (Correspondiente a la 0.99z/0.11.2/0.11.5)**

```
Public Type tCabecera 'Cabecera de los con
    desc As String * 255
    CRC As Long
    MagicWord As Long
End Type

Public Type GrhData
    sX As Integer
    sY As Integer
    FileNum As Integer
    pixelWidth As Integer
    pixelHeight As Integer
    TileWidth As Single
    TileHeight As Single
    
    NumFrames As Integer
    Frames(1 To 25) As Integer
    Speed As Integer
End Type

Public Type Grh

    GrhIndex        As Integer
    FrameCounter    As Single
    Speed           As Single
    Started         As Byte
    Loops           As Integer
    
End Type

Public MiCabecera As tCabecera
Public GrhData() As GrhData

Sub LoadGrhData()
    '*****************************************************************
    'Loads Grh.dat
    '*****************************************************************

    On Error GoTo ErrorHandler

    Dim Grh     As Integer
    Dim Frame   As Integer
    Dim tempint As Integer

    'Resize arrays
    ReDim GrhData(1 To Config_Inicio.NumeroDeBMPs) As GrhData

    'Open files
    Open IniPath & "Graficos.ind" For Binary Access Read As #1
    Seek #1, 1

    Get #1, , MiCabecera
    Get #1, , tempint
    Get #1, , tempint
    Get #1, , tempint
    Get #1, , tempint
    Get #1, , tempint

    'Fill Grh List

    'Get first Grh Number
    Get #1, , Grh

    Do Until Grh <= 0
        
        'Get number of frames
        Get #1, , GrhData(Grh).NumFrames

        If GrhData(Grh).NumFrames <= 0 Then GoTo ErrorHandler
    
        If GrhData(Grh).NumFrames > 1 Then
    
            'Read a animation GRH set
            For Frame = 1 To GrhData(Grh).NumFrames
        
                Get #1, , GrhData(Grh).Frames(Frame)

                If GrhData(Grh).Frames(Frame) <= 0 Or GrhData(Grh).Frames(Frame) > Config_Inicio.NumeroDeBMPs Then
                    GoTo ErrorHandler
                End If
        
            Next Frame
    
            Get #1, , GrhData(Grh).Speed

            If GrhData(Grh).Speed <= 0 Then GoTo ErrorHandler
        
            'Compute width and height
            GrhData(Grh).pixelHeight = GrhData(GrhData(Grh).Frames(1)).pixelHeight

            If GrhData(Grh).pixelHeight <= 0 Then GoTo ErrorHandler
        
            GrhData(Grh).pixelWidth = GrhData(GrhData(Grh).Frames(1)).pixelWidth

            If GrhData(Grh).pixelWidth <= 0 Then GoTo ErrorHandler
        
            GrhData(Grh).TileWidth = GrhData(GrhData(Grh).Frames(1)).TileWidth

            If GrhData(Grh).TileWidth <= 0 Then GoTo ErrorHandler
        
            GrhData(Grh).TileHeight = GrhData(GrhData(Grh).Frames(1)).TileHeight

            If GrhData(Grh).TileHeight <= 0 Then GoTo ErrorHandler
    
        Else
    
            'Read in normal GRH data
            Get #1, , GrhData(Grh).FileNum

            If GrhData(Grh).FileNum <= 0 Then GoTo ErrorHandler
        
            Get #1, , GrhData(Grh).sX

            If GrhData(Grh).sX < 0 Then GoTo ErrorHandler
        
            Get #1, , GrhData(Grh).sY

            If GrhData(Grh).sY < 0 Then GoTo ErrorHandler
            
            Get #1, , GrhData(Grh).pixelWidth

            If GrhData(Grh).pixelWidth <= 0 Then GoTo ErrorHandler
        
            Get #1, , GrhData(Grh).pixelHeight

            If GrhData(Grh).pixelHeight <= 0 Then GoTo ErrorHandler
        
            'Compute width and height
            GrhData(Grh).TileWidth = GrhData(Grh).pixelWidth / TilePixelHeight
            GrhData(Grh).TileHeight = GrhData(Grh).pixelHeight / TilePixelWidth
        
            GrhData(Grh).Frames(1) = Grh
            
        End If

        'Get Next Grh Number
        Get #1, , Grh

    Loop
    '************************************************

    Close #1

    Exit Sub

ErrorHandler:
    Close #1
    MsgBox "Error while loading the Grh.dat! Stopped at GRH number: " & Grh

End Sub
```

---

**LoadGrhData Long Version (Correspondiente a la 0.12.1/0.13)**

```

Public Type GrhData
    sX As Integer
    sY As Integer
    
    FileNum As Long
    
    pixelWidth As Integer
    pixelHeight As Integer
    
    TileWidth As Single
    TileHeight As Single
    
    NumFrames As Integer
    Frames() As Long
    
    Speed As Single
End Type

Public Type Grh
    GrhIndex As Integer
    FrameCounter As Single
    Speed As Single
    Started As Byte
    Loops As Integer
End Type

Public GrhData() As GrhData

Private Function LoadGrhData() As Boolean
On Error GoTo ErrorHandler
    Dim Grh As Long
    Dim Frame As Long
    Dim grhCount As Long
    Dim handle As Integer
    Dim fileVersion As Long
    
    'Open files
    handle = FreeFile()
    Open IniPath & "Graficos.ind" For Binary Access Read As handle
    Seek #1, 1
    
    'Get file version
    Get handle, , fileVersion
    
    'Get number of grhs
    Get handle, , grhCount
    
    'Resize arrays
    ReDim GrhData(1 To grhCount) As GrhData
    
    While Not EOF(handle)
        Get handle, , Grh
        
        With GrhData(Grh)
            'Get number of frames
            Get handle, , .NumFrames
            If .NumFrames <= 0 Then GoTo ErrorHandler
            
            ReDim .Frames(1 To GrhData(Grh).NumFrames)
            
            If .NumFrames > 1 Then
                'Read a animation GRH set
                For Frame = 1 To .NumFrames
                    Get handle, , .Frames(Frame)
                    If .Frames(Frame) <= 0 Or .Frames(Frame) > grhCount Then
                        GoTo ErrorHandler
                    End If
                Next Frame
                
                Get handle, , .Speed
                
                If .Speed <= 0 Then GoTo ErrorHandler
                
                'Compute width and height
                .pixelHeight = GrhData(.Frames(1)).pixelHeight
                If .pixelHeight <= 0 Then GoTo ErrorHandler
                
                .pixelWidth = GrhData(.Frames(1)).pixelWidth
                If .pixelWidth <= 0 Then GoTo ErrorHandler
                
                .TileWidth = GrhData(.Frames(1)).TileWidth
                If .TileWidth <= 0 Then GoTo ErrorHandler
                
                .TileHeight = GrhData(.Frames(1)).TileHeight
                If .TileHeight <= 0 Then GoTo ErrorHandler
            Else
                'Read in normal GRH data
                Get handle, , .FileNum
                If .FileNum <= 0 Then GoTo ErrorHandler
                
                Get handle, , GrhData(Grh).sX
                If .sX < 0 Then GoTo ErrorHandler
                
                Get handle, , .sY
                If .sY < 0 Then GoTo ErrorHandler
                
                Get handle, , .pixelWidth
                If .pixelWidth <= 0 Then GoTo ErrorHandler
                
                Get handle, , .pixelHeight
                If .pixelHeight <= 0 Then GoTo ErrorHandler
                
                'Compute width and height
                .TileWidth = .pixelWidth / TilePixelHeight
                .TileHeight = .pixelHeight / TilePixelWidth
                
                .Frames(1) = Grh
            End If
        End With
    Wend
    
    Close handle
    
    LoadGrhData = True
Exit Function

ErrorHandler:
    LoadGrhData = False
End Function
```



---

**LoadGrhData Long Version (Sin Cabecera) (Correspondiente a AOLibre)**

```
Public Type GrhData
    sX As Integer
    sY As Integer
    
    FileNum As Long
    
    pixelWidth As Integer
    pixelHeight As Integer
    
    TileWidth As Single
    TileHeight As Single
    
    NumFrames As Integer
    Frames() As Long
    
    Speed As Single
End Type

Public Type Grh
    GrhIndex As Long
    FrameCounter As Single
    Speed As Single
    Started As Byte
    Loops As Integer
    angle As Single
End Type

Public Sub LoadGrhData()
On Error GoTo ErrorHandler:

    Dim Grh As Long
    Dim Frame As Long
    Dim grhCount As Long
    Dim handle As Integer
    Dim fileVersion As Long
    
    'Open files
    handle = FreeFile()
    Open IniPath & "Graficos.ind" For Binary Access Read As handle
    
        Get handle, , fileVersion
        
        Get handle, , grhCount
        
        ReDim GrhData(0 To grhCount) As GrhData
        
        While Not EOF(handle)
            Get handle, , Grh
            
            With GrhData(Grh)
            
                '.active = True
                Get handle, , .NumFrames
                If .NumFrames <= 0 Then GoTo ErrorHandler
                
                ReDim .Frames(1 To .NumFrames)
                
                If .NumFrames > 1 Then
                
                    For Frame = 1 To .NumFrames
                        Get handle, , .Frames(Frame)
                        If .Frames(Frame) <= 0 Or .Frames(Frame) > grhCount Then GoTo ErrorHandler
                    Next Frame
                    
                    Get handle, , .Speed
                    If .Speed <= 0 Then GoTo ErrorHandler
                    
                    .pixelHeight = GrhData(.Frames(1)).pixelHeight
                    If .pixelHeight <= 0 Then GoTo ErrorHandler
                    
                    .pixelWidth = GrhData(.Frames(1)).pixelWidth
                    If .pixelWidth <= 0 Then GoTo ErrorHandler
                    
                    .TileWidth = GrhData(.Frames(1)).TileWidth
                    If .TileWidth <= 0 Then GoTo ErrorHandler
                    
                    .TileHeight = GrhData(.Frames(1)).TileHeight
                    If .TileHeight <= 0 Then GoTo ErrorHandler
                    
                Else
                    
                    Get handle, , .FileNum
                    If .FileNum <= 0 Then GoTo ErrorHandler
                    
                    Get handle, , GrhData(Grh).sX
                    If .sX < 0 Then GoTo ErrorHandler
                    
                    Get handle, , .sY
                    If .sY < 0 Then GoTo ErrorHandler
                    
                    Get handle, , .pixelWidth
                    If .pixelWidth <= 0 Then GoTo ErrorHandler
                    
                    Get handle, , .pixelHeight
                    If .pixelHeight <= 0 Then GoTo ErrorHandler
                    
                    .TileWidth = .pixelWidth / TilePixelHeight
                    .TileHeight = .pixelHeight / TilePixelWidth
                    
                    .Frames(1) = Grh
                    
                End If
                
            End With
            
        Wend
    
    Close handle
    
Exit Sub

ErrorHandler:
    
    If Err.number <> 0 Then
        
        If Err.number = 53 Then
            Call MsgBox("El archivo Graficos.ind no existe. Por favor, reinstale el juego.", , "Argentum Online Libre")
            Call CloseClient
        End If
        
    End If
    
End Sub
```