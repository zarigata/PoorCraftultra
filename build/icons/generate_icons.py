import os
import struct
import zlib

BG = (0x36, 0x6C, 0xAA)
FG = (0xF5, 0xF7, 0xFA)
SIZE_MASTER = 512

OUTPUT_DIR = os.path.dirname(os.path.abspath(__file__))


def fill_rect(pixels, x0, y0, x1, y1, color):
    x0 = max(0, min(SIZE_MASTER, x0))
    x1 = max(0, min(SIZE_MASTER, x1))
    y0 = max(0, min(SIZE_MASTER, y0))
    y1 = max(0, min(SIZE_MASTER, y1))
    for y in range(y0, y1):
        row = pixels[y]
        for x in range(x0, x1):
            row[x] = color


def create_master_pixels():
    pixels = [[BG for _ in range(SIZE_MASTER)] for _ in range(SIZE_MASTER)]

    # Letter P
    fill_rect(pixels, 80, 96, 224, 384, FG)
    fill_rect(pixels, 140, 224, 224, 384, BG)
    fill_rect(pixels, 172, 160, 224, 224, BG)

    # Letter C
    fill_rect(pixels, 288, 96, 448, 384, FG)
    fill_rect(pixels, 352, 160, 416, 320, BG)
    fill_rect(pixels, 352, 128, 432, 192, BG)
    fill_rect(pixels, 352, 288, 432, 352, BG)

    return pixels


def resize_nearest(pixels, target_size):
    src_height = len(pixels)
    src_width = len(pixels[0])
    result = []
    for j in range(target_size):
        src_y = int(j * src_height / target_size)
        row = []
        for i in range(target_size):
            src_x = int(i * src_width / target_size)
            row.append(pixels[src_y][src_x])
        result.append(row)
    return result


def png_bytes(pixels):
    height = len(pixels)
    width = len(pixels[0])

    def chunk(chunk_type, data):
        return (
            struct.pack(">I", len(data))
            + chunk_type
            + data
            + struct.pack(">I", zlib.crc32(chunk_type + data) & 0xFFFFFFFF)
        )

    raw_rows = []
    for row in pixels:
        raw_row = bytearray([0])
        for r, g, b in row:
            raw_row.extend((r, g, b))
        raw_rows.append(bytes(raw_row))
    raw = b"".join(raw_rows)
    compressor = zlib.compressobj()
    compressed = compressor.compress(raw) + compressor.flush()

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)

    png_data = [b"\x89PNG\r\n\x1a\n", chunk(b"IHDR", ihdr), chunk(b"IDAT", compressed), chunk(b"IEND", b"")]
    return b"".join(png_data)


def write_file(path, data):
    with open(path, "wb") as fh:
        fh.write(data)


def build_png_variants(master_pixels):
    sizes = [512, 256, 128, 64, 48, 32, 16]
    png_data = {}
    for size in sizes:
        resized = resize_nearest(master_pixels, size)
        data = png_bytes(resized)
        png_path = os.path.join(OUTPUT_DIR, f"poorcraftultra-{size}.png")
        write_file(png_path, data)
        png_data[size] = data
    return png_data


def build_ico(png_data):
    entries = []
    data_blocks = []
    offset = 6 + 16 * len(png_data)
    for size in sorted(png_data):
        data = png_data[size]
        width_byte = size if size < 256 else 0
        entry = struct.pack(
            "<BBBBHHII",
            width_byte,
            width_byte,
            0,
            0,
            1,
            32,
            len(data),
            offset,
        )
        entries.append(entry)
        data_blocks.append(data)
        offset += len(data)

    header = struct.pack("<HHH", 0, 1, len(entries))
    ico_path = os.path.join(OUTPUT_DIR, "poorcraftultra.ico")
    with open(ico_path, "wb") as fh:
        fh.write(header)
        for entry in entries:
            fh.write(entry)
        for block in data_blocks:
            fh.write(block)


def build_icns(png_data):
    mapping = {
        16: b"ic04",
        32: b"ic05",
        64: b"ic06",
        128: b"ic07",
        256: b"ic08",
        512: b"ic09",
    }
    chunks = []
    total_length = 8
    for size in sorted(mapping):
        if size not in png_data:
            continue
        data = png_data[size]
        chunk_type = mapping[size]
        chunk_length = len(data) + 8
        chunk = chunk_type + struct.pack(">I", chunk_length) + data
        chunks.append(chunk)
        total_length += len(chunk)

    icns_path = os.path.join(OUTPUT_DIR, "poorcraftultra.icns")
    with open(icns_path, "wb") as fh:
        fh.write(b"icns" + struct.pack(">I", total_length))
        for chunk in chunks:
            fh.write(chunk)


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    master = create_master_pixels()
    png_data = build_png_variants(master)
    build_ico(png_data)
    build_icns(png_data)
    print("Generated icons in", OUTPUT_DIR)


if __name__ == "__main__":
    main()
