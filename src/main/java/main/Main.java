package main;


import util.CryUtil;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

    private static final File LOCK_FILE = new File(".file_lock");

    private static File lockJarFile;

    private static String jarPath;

    private static String password;

    private static String cpuCode;

    private static String saveCpuCode;

    private static byte[] cryBuff = new byte[1024 * 3];

    private static final boolean IS_LOCK = !LOCK_FILE.exists();

    public static void main(String[] args) {
        jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().toString().replaceAll("file:/", "");
        lockJarFile = new File(jarPath);
        cpuCode = getCpuCode();
        if (!IS_LOCK) {
            try {
                FileReader fileReader = new FileReader(LOCK_FILE);
                char[] buff = new char[100];
                int read = fileReader.read(buff);
                fileReader.close();
                saveCpuCode = new String(buff, 0, read);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        initLockGui();
    }

    private static int getAndCryFiles(File dir, CryUtil cryUtil, boolean isLock) {
        int count = 0;
        File[] files = dir.listFiles(file -> !(file.getAbsolutePath().equals(lockJarFile.getAbsolutePath()) || file.getAbsolutePath().equals(LOCK_FILE.getAbsolutePath())));
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                count += getAndCryFiles(file, cryUtil, isLock);
            }
            cryFile(file, cryUtil, isLock);
            count++;
        }
        return count;
    }

    private static void cryFile(File file, CryUtil cryUtil, boolean isLock) {
        if (file.isFile() && file.length() != 0) {
            Arrays.fill(cryBuff, (byte) 0);
            try {
                RandomAccessFile aFile = new RandomAccessFile(file, "rw");
                long fileLength = aFile.length();
                int read = aFile.read(cryBuff);
                cryUtil.cryBytes(cryBuff, read);
                aFile.seek(0);
                aFile.write(cryBuff, 0, read);
                if (fileLength > 3 * cryBuff.length) {
                    aFile.seek(fileLength - cryBuff.length);
                    aFile.read(cryBuff);
                    cryUtil.cryBytes(cryBuff);
                    aFile.seek(fileLength - cryBuff.length);
                    aFile.write(cryBuff);
                }
                aFile.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String srcFileName = file.getName();
        File parentFile = file.getParentFile();
        File renameFile = new File(parentFile, cryUtil.cryFilenameStr(srcFileName, isLock));
        file.renameTo(renameFile);
    }

    private static JFrame root;
    private static String GUI_TITLE = "文件%s密";
    private static int WINDOW_H = 100;
    private static int WINDOW_W = 300;

    private static void initLockGui() {
        root = new JFrame();
        root.setTitle(String.format(GUI_TITLE, LOCK_FILE.exists() ? "解" : "加"));
        int screenW = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
        int screenH = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
        root.setBounds((screenW - WINDOW_W) / 2, (screenH - WINDOW_H) / 2, WINDOW_W, WINDOW_H);
        root.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel label = new JLabel("密码：");
        JTextField textField = new JTextField(10);
        if (!IS_LOCK) {
//            textField.set
        }
        JButton button = new JButton("确定");
        button.addActionListener(e -> {
            String inputPassword = textField.getText();
            if (inputPassword != null && inputPassword.length() != 0) {
                password = inputPassword;
                CryUtil cryUtil = new CryUtil(password);
                File parentFile = lockJarFile.getParentFile();
                if (!IS_LOCK && !checkPassword(cryUtil)) {
                    JOptionPane.showMessageDialog(root, "密码不正确，解密失败");
                    return;
                } else if (!IS_LOCK && checkPassword(cryUtil) && !LOCK_FILE.delete()) {
                    JOptionPane.showMessageDialog(root, ".file_lock文件删除失败，解密失败");
                    return;
                } else if (IS_LOCK) {
                    writeFileLock(cryUtil);
                }
                button.setEnabled(false);
                textField.setEnabled(false);
                new Thread(() -> {
                    button.setText(String.format("%s密中", IS_LOCK ? "加" : "解"));
                    int count = getAndCryFiles(parentFile, cryUtil, IS_LOCK);
                    int i = 4;
                    while (i > 0) {
                        button.setText(String.format("%s密完成，共%s密%d个项目,%s秒后退出", IS_LOCK ? "加" : "解", IS_LOCK ? "加" : "解", count, i--));
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                    System.exit(0);
                }).start();
            } else {
                JOptionPane.showMessageDialog(root, "密码不能为空");
            }
        });
        JPanel panel = new JPanel();


        panel.add(label);
        panel.add(textField);
        panel.add(button);

        root.getContentPane().add(panel);
        root.setResizable(false);
        root.setVisible(true);
    }

    private static void writeFileLock(CryUtil cryUtil) {
        try {
            FileWriter fileWriter = new FileWriter(LOCK_FILE);
            fileWriter.write(cryUtil.cryStr(cpuCode, true));
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean checkPassword(CryUtil cryUtil) {
        try {
            return cpuCode.equals(cryUtil.cryStr(saveCpuCode, false));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String getCpuCode() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"wmic", "cpu", "get", "ProcessorId"});
            process.getOutputStream().close();
            Scanner sc = new Scanner(process.getInputStream());
            sc.next();
            return sc.next();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}
