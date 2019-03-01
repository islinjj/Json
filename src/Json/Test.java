package Json;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class Test {

	public static void main(String[] args) {
		
		StringBuilder sb = new StringBuilder();
		String s = "";
		
		try (BufferedReader read = new BufferedReader(new FileReader("D:/1.json"))){
			while((s = read.readLine())!=null) {
				sb.append(s);
			}
			
			String json = sb.toString();
			JsonParser parser = new JsonParser(json);
			Object object = parser.parse();
			System.out.println(object);
			
			/**
			 * 利用apche的FileUtils工具读文件
			 */
//			String json = readFile("D:/1.json");
//			JsonParser parser = new JsonParser(json);
//			Object object = parser.parse();
//			System.out.println(object);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

//	private static String readFile(String resource) throws IOException {
//		return FileUtils.readFileToString(
//	            new File(resource));
//	}
}
