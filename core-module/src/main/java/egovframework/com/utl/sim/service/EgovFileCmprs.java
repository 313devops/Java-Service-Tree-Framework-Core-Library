package egovframework.com.utl.sim.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import egovframework.com.cmm.util.EgovResourceCloseHelper;

public class EgovFileCmprs {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EgovFileCmprs.class);

	final static  int COMPRESSION_LEVEL = 8;
	final static  int BUFFER_SIZE = 64 * 1024;
	final static  char FILE_SEPARATOR = File.separatorChar;

	public static boolean cmprsFile(String source, String target) throws Exception {

		// 압축성공여부
		boolean result = false;
		int cnt = 0;
		// 읽어들일 byte 버퍼
		byte[] buffer = new byte[BUFFER_SIZE];

		FileInputStream finput = null;
		FileOutputStream foutput = null;
		ZipOutputStream zoutput = null;

		String source1 = source.replace('\\', FILE_SEPARATOR).replace('/', FILE_SEPARATOR);
		String target1 = target.replace('\\', FILE_SEPARATOR).replace('/', FILE_SEPARATOR);
		File srcFile = new File(source1);

		if (srcFile.exists()) {

			// 1. 파일인 경우
			if (srcFile.isFile()) {

				String target2 = EgovFileTool.createNewFile(target1);
				File tarFile = new File(target2);

				foutput = null;
				zoutput = null;
				ZipEntry zentry = null;

				try {
					foutput = new FileOutputStream(tarFile);
					zoutput = new ZipOutputStream((OutputStream) foutput);
					finput = new FileInputStream(srcFile);
					zentry = new ZipEntry(srcFile.getName());
					zoutput.putNextEntry(zentry);
					zoutput.setLevel(COMPRESSION_LEVEL);
					cnt = 0;
					while ((cnt = finput.read(buffer)) != -1) {
						zoutput.write(buffer, 0, cnt);
					}
					zoutput.closeEntry();
					result = true;
				} catch (IOException e) {
					//2017.03.03 	조성원 	시큐어코딩(ES)-부적절한 예외 처리[CWE-253, CWE-440, CWE-754]
					if(tarFile.delete()){
						LOGGER.debug("[file.delete] tarFile : File Deletion Success");
					}else{						
						LOGGER.error("[file.delete] tarFile : File Deletion Fail");
					}
					throw e;
				} finally {
					EgovResourceCloseHelper.close(finput, zoutput, foutput);
				}

				// 2. 디렉토리인 경우
			} else if (srcFile.isDirectory()) {

				String target2 = EgovFileTool.createNewFile(target1);
				File tarFile = new File(target2);

				ZipEntry zentry = null;

				try {
					foutput = new FileOutputStream(tarFile);
					zoutput = new ZipOutputStream((OutputStream) foutput);
					File[] fileArr = srcFile.listFiles();
					
					//2017.03.03 	조성원 	시큐어코딩(ES)-Null Pointer 역참조[CWE-476]
					if(fileArr == null){
						fileArr = new File[0];
					}
					
					List<String> list = EgovFileTool.getSubFilesByAll(fileArr);
					for (int i = 0; i < list.size(); i++) {
						File sfile = new File((String) list.get(i));
						finput = new FileInputStream(sfile);
						zentry = new ZipEntry(sfile.getAbsolutePath().replace('\\', '/').replaceAll(srcFile.getAbsolutePath().replace('\\', '/'), ""));
						zoutput.putNextEntry(zentry);
						zoutput.setLevel(COMPRESSION_LEVEL);
						cnt = 0;
						while ((cnt = finput.read(buffer)) != -1) {
							zoutput.write(buffer, 0, cnt);
						}
						finput.close();
						result = true;
					}
					
					zoutput.closeEntry();
				} catch (IOException e) {
					tarFile.delete();
					throw e;
				} finally {
					EgovResourceCloseHelper.close(finput, zoutput, foutput);
				}
			}
		}
		return result;
	}

	public static boolean decmprsFile(String source, String target) throws Exception {

		// 압축해제성공여부
		boolean result = false;
		int cnt = 0;
		// 읽어들일 byte 버퍼
		byte[] buffer = new byte[BUFFER_SIZE];

		FileInputStream finput = null;
		FileOutputStream foutput = null;
		ZipInputStream zinput = null;

		String source1 = source.replace('\\', FILE_SEPARATOR).replace('/', FILE_SEPARATOR);
		String target1 = target.replace('\\', FILE_SEPARATOR).replace('/', FILE_SEPARATOR);
		File srcFile = new File(source1);

		if (srcFile.exists() && srcFile.isFile()) {

			String target2 = EgovFileTool.createNewDirectory(target1);
			File tarFile = new File(target2);
			finput = new FileInputStream(srcFile);
			zinput = new ZipInputStream((InputStream) finput);

			ZipEntry entry;

			try {

				while ((entry = zinput.getNextEntry()) != null) {

					String filename = entry.getName();
					String tempFile = EgovFileTool.createNewFile(tarFile.getAbsolutePath() + FILE_SEPARATOR + filename);
					File efile = new File(tempFile);
					foutput = new FileOutputStream(efile);
					while ((cnt = zinput.read(buffer)) != -1) {
						if (foutput != null)
							foutput.write(buffer, 0, cnt);
					}
				}

				result = true;

			} finally {
				EgovResourceCloseHelper.close(finput, zinput, foutput);
			}
		}
		return result;
	}
}
