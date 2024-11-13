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
        String sourceArchivePath = null;
        if (args.length > 0 ) {
            sourceArchivePath = args[0];
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String currentDate = LocalDate.now().format(formatter);

        if (sourceArchivePath == null || sourceArchivePath.isEmpty()) {
            sourceArchivePath = getFilePath();
            sourceArchivePath = "C:\\Projects\\student\\Archivator test\\archives\\zip.zip";
        }
        String tempDir = unzipFile(sourceArchivePath, currentDate);
        zipAndSaveOver1MBFiles(currentDate);
        zipAndSaveLess1MBFiles(tempDir);
    }

    private static String unzipFile(String sourceArchivePath, String currentDate) throws IOException {
        byte[] buffer = new byte[1024];

        ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceArchivePath));
        ZipEntry ze = zis.getNextEntry();

        File unzippedDirectoryLess1MB = null;
        File unzippedDirectoryOver1MB = new File("RESULT_" + currentDate);

        while (ze != null) {
            System.out.println(ze.getName());
            System.out.printf("%.2f MB\n", (ze.getSize() / 1024.0 / 1024.0));
            File newFile;
            if ((ze.getSize() / 1024.0 / 1024.0) < 1) {
                if (unzippedDirectoryLess1MB == null) {
                    unzippedDirectoryLess1MB = new File(ze.getName().substring(0, ze.getName().lastIndexOf('.')));
                }
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
        return unzippedDirectoryLess1MB.getName();
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

    private static void zipAndSaveLess1MBFiles(String tempDir) throws IOException, SQLException {
        File unzippedDirectoryLess1MB = new File(tempDir);
        File zippedDirectoryLess1MB = new File(tempDir + ".zip");

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zippedDirectoryLess1MB.getAbsolutePath()));
        File directoryToZip = new File(unzippedDirectoryLess1MB.getAbsolutePath());
        zipFile(directoryToZip, directoryToZip.getName(), zos);
        zos.close();
        String sql = "insert into archives(filename, filesource) values(?, ?)";
        Connection conn = DriverManager.getConnection(Driver, User, Password);
        PreparedStatement statement = conn.prepareStatement(sql);
        ByteArrayInputStream bais = new ByteArrayInputStream(getByteArray(zippedDirectoryLess1MB));
        statement.setString(1, tempDir);
        statement.setBinaryStream(2, bais);
        statement.executeUpdate();
        conn.close();

        String[] entries = unzippedDirectoryLess1MB.list();
        for (String entrie : entries) {
            File currentFile = new File(unzippedDirectoryLess1MB, entrie);
            currentFile.delete();
        }
        unzippedDirectoryLess1MB.delete();
    }

    private static void zipAndSaveOver1MBFiles(String currentDate) throws IOException {

        File unzippedDirectoryOver1MB = new File("RESULT_" + currentDate);
        File zippedDirectoryOver1MB = new File("RESULT_" + currentDate + ".zip");

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zippedDirectoryOver1MB.getAbsolutePath()));
        File directoryToZip = new File(unzippedDirectoryOver1MB.getAbsolutePath());
        zipFile(directoryToZip, directoryToZip.getName(), zos);
        zos.close();

        String[] entries = unzippedDirectoryOver1MB.list();
        for (String entrie : entries) {
            File currentFile = new File(unzippedDirectoryOver1MB, entrie);
            currentFile.delete();
        }
        unzippedDirectoryOver1MB.delete();
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