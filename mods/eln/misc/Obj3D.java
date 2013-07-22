package mods.eln.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;



import mods.eln.Eln;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

public class Obj3D {
	String fileName;
	ArrayList<Vertex> vertex = new ArrayList<Vertex>();
	ArrayList<Uv> uv = new ArrayList<Uv>();	
	
	Hashtable<String, String> nameToStringHash = new Hashtable<String, String>();

	
	public class Obj3DPart
	{
		boolean listReady = false;
		int glList;
		ArrayList<Vertex> vertex;
		ArrayList<Uv> uv;
		ArrayList<Face> face = new ArrayList<Face>();
		Hashtable<String, Float> nameToFloatHash = new Hashtable<String, Float>();

		String mtlName;
		public ResourceLocation textureResource;
		public Obj3DPart(ArrayList<Vertex> vertex,ArrayList<Uv> uv) {
			this.vertex = vertex;
			this.uv = uv;
		}
		/*
		
	    private FloatBuffer vertices;
	    private IntBuffer indices;
	    private int VBOVertices;
	    private int VBOIndices;
		
	
	    public int createVBOID() {
    	  if (GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
    	    IntBuffer buffer = BufferUtils.createIntBuffer(1);
    	    ARBVertexBufferObject.glGenBuffersARB(buffer);
    	    return buffer.get(0);
    	  }
    	  return 0;
    	}	    
    
	    public void bufferData(int id, FloatBuffer buffer) {
    	  if (GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
    	    ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, id);
    	    ARBVertexBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, buffer, ARBVertexBufferObject.GL_STATIC_DRAW_ARB);
    	  }
    	}
	    public void bufferElementData(int id, IntBuffer buffer) {
		  if (GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
		    ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, id);
		    ARBVertexBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, buffer, ARBVertexBufferObject.GL_STATIC_DRAW_ARB);
		  }
    	}
	    
	    public void render() {
	    	  GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
	    	  ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, VBOVertices);
	    	  GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, 0);
	    	 
	    	  //GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
	    	//  ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, colourBufferID);
	    	//  GL11.glColorPointer(4, GL11.GL_FLOAT, 0, 0);
	    	 
	    	  ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ELEMENT_ARRAY_BUFFER_ARB, VBOIndices);
	    	  GL12.glDrawRangeElements(GL11.GL_TRIANGLES, 0, 3, indices);
	    	}*/
		
		public void drawNoBind(){
			if(listReady == false)
			{
				listReady = true;
				glList = GL11.glGenLists(1);
				
				GL11.glNewList(glList, GL11.GL_COMPILE);
				drawVertex();
				GL11.glEndList();					
				/*
				
		        float[] vertexArray = {-0.5f,  0.5f, 0,
                        0.5f,  0.5f, 0,
                        0.5f, -0.5f, 0,
                       -0.5f, -0.5f, 0};
				vertices = BufferUtils.createFloatBuffer(vertexArray.length);
				vertices.put(vertexArray);
				vertices.flip();
				
				int[] indexArray = {0, 1, 2, 0, 2, 3};
				indices = BufferUtils.createIntBuffer(indexArray.length);
				indices.put(indexArray);
				indices.flip();
				
				
				VBOVertices = createVBOID();
				VBOIndices = createVBOID();
				bufferData(VBOVertices, vertices);
				bufferElementData(VBOIndices,indices);

				*/
				
			}

			
			GL11.glCallList(glList);			
		}
		public void draw()
		{		
			if(textureResource != null){
				Utils.bindTexture(textureResource);
				drawNoBind();
			}
			else {
				GL11.glDisable(GL11.GL_TEXTURE_2D);
				drawNoBind();
				GL11.glEnable(GL11.GL_TEXTURE_2D);
			}
			
		}
		
		public void bindTexture()
		{
			Utils.bindTexture(textureResource);
		}
		

		public float getFloat(String name)
		{
			return nameToFloatHash.get(name);
		}
		/*
		public static int createVBOID() {
			if (GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
			IntBuffer buffer = BufferUtils.createIntBuffer(1);
			ARBVertexBufferObject.glGenBuffersARB(buffer);
			return buffer.get(0);
			}
			return 0;
		}*/
		float ox,oy,oz;
		public void draw(float angle,float x,float y,float z)
		{
			GL11.glPushMatrix();
			
			GL11.glTranslatef(ox,oy,oz);
			GL11.glRotatef(angle,x,y,z);
			GL11.glTranslatef(-ox,-oy,-oz);
			draw();
			
			GL11.glPopMatrix();
		}
		public void drawNoBind(float angle,float x,float y,float z)
		{
			GL11.glPushMatrix();
			
			GL11.glTranslatef(ox,oy,oz);
			GL11.glRotatef(angle,x,y,z);
			GL11.glTranslatef(-ox,-oy,-oz);
			drawNoBind();
			
			GL11.glPopMatrix();
		}
		
		private void drawVertex()
		{
			//float dx = 0,dy = 0,dz = 0;
			//if(nameToFloatHash.containsKey("offsetX")) dx = nameToFloatHash.get("offsetX");
			//if(nameToFloatHash.containsKey("offsetY")) dy = nameToFloatHash.get("offsetY");
			//if(nameToFloatHash.containsKey("offsetZ")) dz = nameToFloatHash.get("offsetZ");
			int mode = 0;
		/*	Minecraft.getMinecraft().renderEngine.resetBoundTexture();
			if(textureName != null) Utils.bindTextureByName(textureName);
			*/
			for(Face f : face)
			{
				if(f.vertexNbr!=mode)
				{
					if(mode != 0) GL11.glEnd();
					switch(f.vertexNbr)
					{
					case 3:
						GL11.glBegin(GL11.GL_TRIANGLES);
						break;
					case 4:
						GL11.glBegin(GL11.GL_QUADS);
						break;
					case 6:
					//	GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
						break;
					case 8:
					//	GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
						break;
					}
					
					mode = f.vertexNbr;
				}
				
				GL11.glNormal3f(f.normal.x, f.normal.y, f.normal.z);
				for(int idx = 0;idx<mode;idx++)
				{
					if(f.uv[idx] != null) GL11.glTexCoord2f(f.uv[idx].u,f.uv[idx].v);
					GL11.glVertex3f(f.vertex[idx].x,f.vertex[idx].y, f.vertex[idx].z);
				}
			}
			
			if(mode != 0) GL11.glEnd();
		}
		
		
	}
	Hashtable<String, Obj3DPart> nameToPartHash = new Hashtable<String, Obj3DPart>();

	
	
	class Vertex{
		Vertex(float x,float y,float z)
		{
			this.x = x;this.y = y;this.z = z;
		}
		Vertex(String[] value)
		{
			x = Float.parseFloat(value[0]);
			y = Float.parseFloat(value[1]);
			z = Float.parseFloat(value[2]);
		}
		


		public float x,y,z;
	}
	class Uv{
		Uv(float u,float v)
		{
			this.u = u;this.v = v;
		}
		Uv(String[] value)
		{
			u = Float.parseFloat(value[0]);
			v = Float.parseFloat(value[1]);
		}
		public float u,v;
	}
	class Normal{
		Normal(float x,float y,float z)
		{
			this.x = x;this.y = y;this.z = z;
		}
		Normal(String[] value)
		{
			x = Float.parseFloat(value[0]);
			y = Float.parseFloat(value[1]);
			z = Float.parseFloat(value[2]);
		}
		Normal(Vertex o,Vertex a,Vertex b)
		{
			float a_x = a.x-o.x;
			float a_y = a.y-o.y;
			float a_z = a.z-o.z;
			
			float b_x = b.x-o.x;
			float b_y = b.y-o.y;
			float b_z = b.z-o.z;

			
			x = a_y*b_z - a_z*b_y;
			y = a_z*b_x - a_x*b_z;
			z = a_x*b_y - a_y*b_x;
			
			float norme = (float) Math.sqrt(x*x + y*y + z*z);
			
			x/=norme;
			y/=norme;
			z/=norme;
		}
		public float x,y,z;
	}
	class Face{
		Face(Vertex[] vertex,Uv[] uv,Normal normal)
		{
			this.vertex = vertex;this.uv = uv;this.normal = normal;
			vertexNbr = vertex.length;
		}
		
		
		public Vertex[] vertex;
		public Uv[] uv;
		Normal normal;
		public int vertexNbr;
	}
	
	String mtlName = null;
	
	public Obj3D() {
	}
	
	public ResourceLocation getAlternativeTexture(String name)
	{
		ResourceLocation resource = new ResourceLocation(mod,directory + name);
		return resource;
	}
//	static final String rootDirectory = "/mods/eln/model/";
	
	String directory;
	String mod;
	public void loadFile(String modName,String path)
	{
		int lastSlashId = path.lastIndexOf('/');
		this.directory = path.substring(0, lastSlashId + 1);
		this.fileName = path.substring(lastSlashId + 1,path.length());
		Obj3DPart part = null;
		mod = modName;
		try {
			
		//	File f  = Minecraft.getAppDir("../src/minecraft");	

			{
				//ITexturePack var6 = Minecraft.getMinecraft().renderEngine.texturePack.getSelectedTexturePack(); 
				//InputStream stream = var6.getResourceAsStream(/*rootDirectory + */directory + fileName);
				InputStream stream = Eln.class.getResourceAsStream("/assets/" + modName + directory + fileName);
				
				//InputStream stream =  new  FileInputStream(path);
				//InputStream stream = Eln.class.getResourceAsStream("/Eln/model/MONKEY.obj");//(directory + fileName);	
				StringBuilder inputStringBuilder = new StringBuilder();
		        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));

		        String line;
		        
		        while((line = bufferedReader.readLine()) != null)
				{
					String[] words = line.split(" ");	
					if(words[0].equals("o"))
					{
						part = new Obj3DPart(vertex,uv);
						nameToPartHash.put(words[1], part);
					}
					else if(words[0].equals("v"))
					{
						part.vertex.add(new Vertex(	Float.parseFloat(words[1]),
												Float.parseFloat(words[2]),
												Float.parseFloat(words[3])
									));
					}
					else if(words[0].equals("vt"))
					{
						part.uv.add(new Uv(	Float.parseFloat(words[1]),
										1-Float.parseFloat(words[2])
								));				
					}
					else if(words[0].equals("f"))
					{
						int vertexNbr = words.length - 1;
						if(vertexNbr == 3)
						{
							Vertex[] verticeId = new Vertex[vertexNbr];
							Uv[] uvId = new Uv[vertexNbr];
							for(int idx = 0;idx<vertexNbr;idx++)
							{
								String[] id = words[idx+1].split("/");
	
								verticeId[idx] = part.vertex.get(Integer.parseInt(id[0]) - 1);
								if(id.length > 1 && ! id[1].equals(""))
								{
									uvId[idx] = part.uv.get(Integer.parseInt(id[1]) - 1);
								}
								else
								{
									uvId[idx] = null;
								}
							}
									
							//System.out.println(vertexNbr  + " " +  uvId + " " +  verticeId[0] + " " + verticeId[1] + " " + verticeId[2] + " ");
							part.face.add(new Face(verticeId, uvId, new Normal(verticeId[0], verticeId[1],verticeId[2])));
						}
						else
						{
							System.out.println("obj assert vertexNbr != 3");
						}
					}
					else if(words[0].equals("mtllib"))
					{
						mtlName = words[1];
					}
					else if(words[0].equals("usemtl"))
					{
						part.mtlName = words[1];
					}
				}
			}
			part = null;
			{		 
				InputStream stream = Eln.class.getResourceAsStream("/assets/" + modName + directory +  mtlName);	
				StringBuilder inputStringBuilder = new StringBuilder();
		        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
		        String mtlName = "";
		        String line;
		        while((line = bufferedReader.readLine()) != null)
				{
					String[] words = line.split(" ");	
					if(words[0].equals("newmtl"))
					{
						mtlName = words[1];

					}
					else if(words[0].equals("map_Kd"))
					{
						for(Obj3DPart partPtr : nameToPartHash.values())
						{
							if(partPtr.mtlName.equals(mtlName))
							{
								part = partPtr;
								part.textureResource = new ResourceLocation(modName, directory + words[1]);
								//Side side = FMLCommonHandler.instance().getEffectiveSide();
								//if (side == Side.CLIENT)
									//MinecraftForgeClient.preloadTexture(part.textureName);

							}
						}
					}

				}	
			}
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		part = null;
		
		try {		 
			InputStream stream = Eln.class.getResourceAsStream("/assets/" + modName + directory +  fileName.replace(".obj", ".txt").replace(".OBJ", ".txt"));	
			if(stream != null)
			{
				StringBuilder inputStringBuilder = new StringBuilder();
		        BufferedReader bufferedReader;
				
				bufferedReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));

	
		        String line;
		        while((line = bufferedReader.readLine()) != null)
				{
					String[] words = line.split(" ");	
					if(words[0].equals("o"))
					{
						part = nameToPartHash.get(words[1]);
					}
					else if(words[0].equals("f"))
					{
						if(words[1].equals("originX"))
						{
							part.ox = Float.valueOf(words[2]);
						}
						else if(words[1].equals("originY"))
						{
							part.oy = Float.valueOf(words[2]);
						}
						else if(words[1].equals("originZ"))
						{
							part.oz = Float.valueOf(words[2]);
						}
						else 
						{
							part.nameToFloatHash.put(words[1], Float.valueOf(words[2]));
						}
					}
					else if(words[0].equals("s"))
					{
						nameToStringHash.put(words[1], words[2]);
					}

				}	
			}
		
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*
		for(Obj3DPart partPtr : nameToPartHash.values())
		{
			partPtr.glList = GL11.glGenLists(1);
			
			GL11.glNewList(partPtr.glList, GL11.GL_COMPILE);
			partPtr.draw();
			GL11.glEndList();			
		}*/

	}
	

	
	public Obj3DPart getPart(String part)
	{
		return nameToPartHash.get(part);
	}
	public void draw(String part)
	{ 
		Obj3DPart partPtr = getPart(part);
		if(partPtr != null) partPtr.draw();
	}
	
	public String getString(String name)
	{
		return nameToStringHash.get(name);
	}
}
