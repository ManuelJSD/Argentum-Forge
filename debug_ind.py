
import struct
import os

file_path = r"F:\Repositorio Argentum\Argentum-Online\client\INIT\Graficos.ind"

try:
    with open(file_path, "rb") as f:
        data = f.read()
        print(f"File size: {len(data)} bytes")
        
        if len(data) < 8:
            print("Error: File too short to contain header (version + count)")
            exit(1)
            
        version = struct.unpack_from("<l", data, 0)[0]
        count = struct.unpack_from("<l", data, 4)[0]
        
        print(f"Version: {version}")
        print(f"GrhCount: {count}")
        
        # Estimate required size min
        # Min bytes per entry approx 18 bytes.
        estimated_min_size = 8 + (count * 12) # extremely conservative
        print(f"Estimated min size for {count} entries: {estimated_min_size}")
        
        if len(data) < estimated_min_size:
             print("WARNING: File size is significantly smaller than expected for this count.")
             
except Exception as e:
    print(f"Error reading file: {e}")
