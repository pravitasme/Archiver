package org.example;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class App {

    static final String Driver = "jdbc:postgresql://localhost:5432/postgres";
    static final String User = "postgres";
    static final String Password = "1324";

    public static void main(String[] args) throws IOException, SQLException {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String currentDate = LocalDate.now().format(formatter);

        String sourceArchivePath = getFilePath();
        sourceArchivePath = "C:\\Projects\\student\\Archivator test\\archives\\zip.zip";

        File unzippedDirectoryLess1MB = new File("C:\\Projects\\student\\Archivator test\\unzipped files\\less than 1 MB");
        File unzippedDirectoryOver1MB = new File("C:\\Projects\\student\\Archivator test\\unzipped files\\over 1 MB");

        File zippedDirectoryLess1MB = new File("C:\\Projects\\student\\Archivator test\\zipped files\\less.zip");
        File zippedDirectoryOver1MB = new File("C:\\Projects\\student\\Archivator test\\zipped files\\RESULT_" + currentDate + ".zip");

        unzipFile(sourceArchivePath, unzippedDirectoryLess1MB, unzippedDirectoryOver1MB);
        zipAndSaveOver1MBFiles(zippedDirectoryOver1MB, unzippedDirectoryOver1MB);
        zipAndSaveLess1MBFiles(zippedDirectoryLess1MB, unzippedDirectoryLess1MB);
    }

    private static void unzipFile(String sourceArchivePath, File unzippedDirectoryLess1MB, File unzippedDirectoryOver1MB) throws IOException {
        byte[] buffer = new byte[1024];

        ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceArchivePath));
        ZipEntry ze = zis.getNextEntry();

        while (ze != null) {
            System.out.println(ze.getName());
            System.out.printf("%.2f MB\n", (ze.getSize() / 1024.0 / 1024.0));
            File newFile;
            if ((ze.getSize() / 1024.0 / 1024.0) < 1) {
                newFile = new File(unzippedDirectoryLess1MB, ze.getName());
            }
            else {
                newFile = new File(unzippedDirectoryOver1MB, ze.getName());
            }

            if (ze.isDirectory()) {
                newFile.mkdirs();
            }
            else {
                File parent = newFile.getParentFile();
                if (!parent.isDirectory()) {
                    parent.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            ze = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    private static String getFilePath() {
        Scanner sc = new Scanner(System.in);
        String filePath;
        System.out.println("Enter the file path: ");
        filePath = sc.nextLine().replaceAll("\"", "");
        return filePath;
    }

    private static void zipFile(File directoryToZip, String zipFileName, ZipOutputStream zos) throws IOException {
        if (directoryToZip.isDirectory()) {
            if (zipFileName.endsWith("/")) {
                zos.putNextEntry(new ZipEntry(zipFileName));
                zos.closeEntry();
            }
            else {
                zos.putNextEntry(new ZipEntry(zipFileName + "/"));
                zos.closeEntry();
            }
            File[] children = directoryToZip.listFiles();
            for (File child : children) {
                zipFile(child, zipFileName + "/" + child.getName(), zos);
            }
        }
        else {
            FileInputStream fis = new FileInputStream(directoryToZip);
            ZipEntry ze = new ZipEntry(zipFileName);
            zos.putNextEntry(ze);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            fis.close();
        }
    }

    private static void zipAndSaveLess1MBFiles(File zippedDirectoryLess1MB, File unzippedDirectoryLess1MB) throws IOException, SQLException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zippedDirectoryLess1MB.getAbsolutePath()));
        File directoryToZip = new File(unzippedDirectoryLess1MB.getAbsolutePath());
        zipFile(directoryToZip, directoryToZip.getName(), zos);
        zos.close();
        String sql = "insert into archives(filename, filesource) values(?, ?)";
        Connection conn = DriverManager.getConnection(Driver, User, Password);
        PreparedStatement statement = conn.prepareStatement(sql);
        ByteArrayInputStream bais = new ByteArrayInputStream(getByteArray(zippedDirectoryLess1MB));
        statement.setString(1, "meme");
        statement.setBinaryStream(2, bais);
        statement.executeUpdate();
        conn.close();
    }

    private static void zipAndSaveOver1MBFiles(File zippedDirectoryOver1MB, File unzippedDirectoryOver1MB) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zippedDirectoryOver1MB.getAbsolutePath()));
        File directoryToZip = new File(unzippedDirectoryOver1MB.getAbsolutePath());
        zipFile(directoryToZip, directoryToZip.getName(), zos);
        zos.close();
    }

    private static byte[] getByteArray(File file) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        InputStream in = new FileInputStream(file);
        byte[] buffer = new byte[1024];

        int len;
        while ((len = in.read(buffer)) > 0) {
            bos.write(buffer, 0, len);
        }
        in.close();

        return bos.toByteArray();
    }
}
