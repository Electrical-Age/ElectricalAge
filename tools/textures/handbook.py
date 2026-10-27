"""The Electrical Engineer's Ledger (item/handbook.png): a ledger bound in black rubber, a cream label, copper wire
wound round it twice, pages showing on the right. Usage: python3 handbook.py OUT.png"""
import sys
from PIL import Image

W = H = 16
img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
px = img.load()
COVER, COVER_HI, COVER_LO = (38, 36, 40), (62, 60, 66), (22, 21, 24)
PAGE, PAGE_LO = (226, 218, 196), (184, 174, 150)
LABEL, INK = (214, 196, 150), (90, 64, 40)
CU, CU_HI, CU_LO = (196, 108, 52), (238, 160, 96), (130, 64, 30)

# book body: x 2..13, y 1..14; pages on the right edge and bottom
for y in range(1, 15):
    for x in range(2, 14):
        px[x, y] = COVER
for y in range(2, 15):
    px[13, y] = PAGE if y % 2 else PAGE_LO
    px[12, 14] = PAGE_LO
for x in range(3, 13):
    px[x, 14] = PAGE if x % 2 else PAGE_LO
# outline and highlight
for y in range(1, 15):
    px[2, y] = COVER_LO
for x in range(2, 13):
    px[x, 1] = COVER_LO
for y in range(2, 14):
    px[3, y] = COVER_HI          # spine highlight
# label with two ink lines
for y in range(4, 9):
    for x in range(5, 11):
        px[x, y] = LABEL
for x in range(6, 10):
    px[x, 5] = INK
for x in range(6, 9):
    px[x, 7] = INK
# copper wire wound round twice (rows 9 and 11), with a twist
for y in (10, 12):
    for x in range(2, 14):
        px[x, y] = CU if x % 3 else CU_HI
    px[2, y] = CU_LO
px[13, 11] = CU_LO
px[1, 11] = CU
px[0, 12] = CU_HI
img.save(sys.argv[1])
