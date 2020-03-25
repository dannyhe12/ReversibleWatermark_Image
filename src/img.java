import java.util.*;
import java.io.*;
import org.opencv.core.Core;

public class img {
	private static calctool Kit = new calctool();
	
	public static void main(String[] args) throws Exception {
//		byte[] file;byte[] water;
//		// TODO Auto-generated method stub
//		
//			file = readStream("Lena");
//			water = readStream("Lena");}
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		String imagePath = "E:\\yuncong\\src\\img\\001.jpeg";//载体图像
		String waterPath = "anymark";
		String foldname = "E:\\yuncong\\src\\img";//将含密图像保存到这个目录下
		String saveAs = "watermarked.jpeg";//含密名称

		String recoverFold = "F:\\extracted.jpeg"; //恢复图像目录

		if (imagePath.endsWith("jpg") || imagePath.endsWith("jpeg")){
			int[][] newA0 = null;int[][] newA1 = null;int password = 0;
			try {
				InputStream in = new FileInputStream("src\\16.txt");
				ObjectInputStream oin = new ObjectInputStream(in);
				newA0 = (int[][]) oin.readObject();
				in = new FileInputStream("src\\17.txt");
				oin = new ObjectInputStream(in);
				newA1 = (int[][]) oin.readObject();
			}catch(Exception e){
				System.out.println("[Error] Loading Self-defined Huffman Tree Failed!");
				e.printStackTrace();
			}


			jpeglibrary a = new jpeglibrary(imagePath,null,null,null,waterPath,null,3,password,newA0,newA1);
			//信息隐藏，得到含密图像
			String hidePath = a.hide(foldname,saveAs);
			//信息提取与图像恢复，恢复出的原始图像保存到foldname下
			List<String> result = a.extract(hidePath,recoverFold);

			String water = result.get(0);String image_path = result.get(1);
			System.out.println("------------------------");
			System.out.println("watermark： "+water);
			System.out.println("saved path： "+image_path);
		}

		//除jpeg外其他格式图片
		else{
			//信息隐藏，得到含密图像
			libraryEmbed a = new libraryEmbed(imagePath, waterPath);
			String hidePath = a.hide(foldname, saveAs);

			////信息提取与图像恢复，恢复出的原始图像保存到foldname下
			libraryExtract b = new libraryExtract();
			List<String> result = b.extract(hidePath,recoverFold);
			String water = result.get(0);
			String image_path = result.get(1);
			System.out.println("------------------------");
			System.out.println("watermark： "+water);
			System.out.println("saved path： "+image_path);
		}
//		System.out.println("英文参考： "+result.get(0));
//		System.out.println("中文参考： "+result.get(1));
		
//		StringBuilder a = new StringBuilder();
//		add(a);
//		System.out.println(a.toString());
//		byte[] compressedBytes= new byte[1024];
//		
//		JPEGLosslessDecoder decoder = new JPEGLosslessDecoder(compressedBytes);


//		char[] a = new char[2];int b=1;
//		char a=(char)1;
//		System.out.println(a);
//		ArrayList a = new ArrayList();
//		a.add(2);
//		System.out.println(a.size());
//		byte b = (byte) 247;
//        String s=  Kit.byteToBit(b);
//        
//        System.out.println((int)s.charAt(2));
	}
	
	public static void add(StringBuilder b){b.append('1');}
	

	

}
