package task.work.main;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import task.db.TaskStringDB;
import task.work.TaskFile;
import task.work.TaskString;
import task.work.Utils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.Scanner;

public class Main {
    private static int numberOfFiles = 100;
    private static String filesFolderPath = Utils.getFilesFolder();

    private static ApplicationContext applicationContext = new ClassPathXmlApplicationContext("Beans.xml");
    private static TaskStringDB taskStringDB = (TaskStringDB) applicationContext.getBean("taskStringDB");

    public static void main(String[] args) {
        System.out.println("Program operations:");
        System.out.println("1. Create 100 files");
        System.out.println("2. Combine all files into 1");
        System.out.println("3. Import to database management system");
        System.out.println("0. Exit");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nEnter the number of your operation: ");
            var userCase = scanner.nextInt();

            switch (userCase) {
                case 1:
                    createFiles();
                    break;
                case 2:
                    combineFiles();
                    break;
                case 3:
                    insertToDB();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Please, enter number from the menu!");
            }
        }
    }

    private static void createFiles() {
        Utils.createFileFolder();

        TaskFile file = new TaskFile();
        for (var i = 0; i < numberOfFiles; i++) {
            file.createFile(i + 1);
            System.out.print("\rCreating files: " + (int) percentOfProcessedFiles(i + 1) + "%");
        }
    }

    private static void combineFiles() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter character combination you want to delete (if no, just press 'enter'): ");
        var charCombination = scanner.nextLine();
        if (charCombination.isEmpty()) {
            combineFilesWithoutRemoving();
        } else {
            combineFilesWithRemoving(charCombination);
        }
    }

    private static void combineFilesWithRemoving(String charCombination) {
        FileWriter newFile = null;
        try {
            newFile = new FileWriter(filesFolderPath + File.separator + "combined.txt", true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int numberOfDeletedStrings = 0;
        String newLine;
        BufferedReader bufferedReader;

        for (var i = 1; i <= numberOfFiles; i++) {
            var fileName = filesFolderPath + File.separator + i + ".txt";
            try {
                bufferedReader = new BufferedReader(new FileReader(fileName));
                while ((newLine = bufferedReader.readLine()) != null) {
                    if (!newLine.contains(charCombination)) {
                        newFile.append(newLine);
                        newFile.append("\n");
                    } else {
                        numberOfDeletedStrings++;
                    }
                    deleteFile(fileName);
                }

                System.out.print("\rFiles processed: " + (int) percentOfProcessedFiles(i) + "%");
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("\nThe number of deleted lines is " + numberOfDeletedStrings);
    }

    private static void combineFilesWithoutRemoving() {
        FileChannel sourceChannel, destinationChannel = null;
        var writeFileName = filesFolderPath + File.separator + "combined.txt";
        try {
            destinationChannel = new FileOutputStream(writeFileName).getChannel();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        long position = 0;
        long size;

        for (var i = 1; i <= numberOfFiles; i++) {
            var readFileName = filesFolderPath + File.separator + i + ".txt";
            try {
                sourceChannel = new FileInputStream(readFileName).getChannel();
                size = sourceChannel.size();
                destinationChannel.transferFrom(sourceChannel, position, size);
                position += size;
                deleteFile(readFileName);

                System.out.print("\rFiles processed: " + (int) percentOfProcessedFiles(i) + "%");
                sourceChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            destinationChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void insertToDB() {
        long stringsImported = 0;
        TaskString taskString = new TaskString();

        File folder = new File(filesFolderPath);
        BufferedReader bufferedReader = null;

        if (folder.length() > 1) {
            for (var i = 1; i <= numberOfFiles; i++) {
                var fileName = filesFolderPath + File.separator + i + ".txt";
                try {
                    bufferedReader = new BufferedReader(new FileReader(fileName));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                String newLine = "";
                while (true) {
                    try {
                        if (!((newLine = bufferedReader.readLine()) != null)) break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        taskString = new TaskString(newLine);
                    } catch (NullPointerException npe) {
                        npe.printStackTrace();
                    }
                    taskStringDB.insert(taskString);
                    stringsImported++;
                    System.out.print("\rFile #" + i +
                            ", strings imported: " + stringsImported +
                            ", strings left: " + ((numberOfFiles * TaskFile.numberOfStrings) - stringsImported));
                }
            }
        } else {
            System.out.println("\nCreate files to insert them into DB");
        }
    }

    private static float percentOfProcessedFiles(int fileNumber) {
        return ((float) fileNumber / (float) numberOfFiles) * 100;
    }

    private static boolean deleteFile(String fileName) {
        File file = new File(fileName);
        return file.delete();
    }
}