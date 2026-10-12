import unittest
import verify_cleanroom_render_assets as assets
class ConnectedOreAssetsTest(unittest.TestCase):
 def test_every_neighbor_pattern_keeps_its_boundary_and_missing_corner(self):
  pixels=assets.connected_pixels()
  self.assertEqual(len(pixels),256*256*4)
  for mask in range(256):
   tileX=(mask%16)*16;tileY=(mask//16)*16
   at=lambda x,y:pixels[((tileY+y)*256+tileX+x)*4+3]
   for bit,point in enumerate(((8,0),(15,8),(8,15),(0,8))):
    self.assertEqual(at(*point),0 if mask&(1<<bit) else 255)
   for bit,point in enumerate(((15,0),(15,15),(0,15),(0,0))):
    complete=(1<<bit)|(1<<((bit+1)%4))|(1<<(bit+4))
    self.assertEqual(at(*point),0 if mask&complete==complete else 255)
 def test_isolated_matches_the_existing_two_pixel_border(self):
  pixels=assets.connected_pixels()
  for y in range(16):
   for x in range(16):
    alpha=pixels[(y*256+x)*4+3]
    self.assertEqual(alpha,255 if min(x,y,15-x,15-y)<2 else 0)
 def test_indexed_png_preserves_every_rgba_pixel(self):
  import struct,zlib
  png=assets.generated_connected_texture();chunks={};cursor=8
  while cursor<len(png):
   size=struct.unpack(">I",png[cursor:cursor+4])[0];tag=png[cursor+4:cursor+8];data=png[cursor+8:cursor+8+size]
   self.assertEqual(zlib.crc32(tag+data),struct.unpack(">I",png[cursor+8+size:cursor+12+size])[0]);chunks[tag]=data;cursor+=size+12
  self.assertEqual(struct.unpack(">IIBBBBB",chunks[b"IHDR"]),(256,256,1,3,0,0,0))
  raw=zlib.decompress(chunks[b"IDAT"]);self.assertEqual(len(raw),256*33);decoded=bytearray()
  for y in range(256):
   self.assertEqual(raw[y*33],0)
   for x in range(256):
    index=(raw[y*33+1+x//8]>>(7-x%8))&1
    decoded.extend(chunks[b"PLTE"][index*3:index*3+3]);decoded.append(chunks[b"tRNS"][index])
  self.assertEqual(decoded,assets.connected_pixels())
if __name__=='__main__':unittest.main()
