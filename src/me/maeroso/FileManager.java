package me.maeroso;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class FileManager {
    private static FileManager ourInstance = new FileManager();
    private List<File> fileList;
    private File filesRepo;

    private FileManager() {
        this.filesRepo = new File(String.format("%s@%d", Configuration.DEFAULT_FOLDER, PeerManager.getInstance().getOurPeer().getPort()));
        if (!this.filesRepo.exists()) {
            this.filesRepo.mkdirs();
        }
        this.fileList = new LinkedList<>();
    }

    public static FileManager getInstance() {
        return ourInstance;
    }

    public List<File> search(String fileName) {
        return fileList.stream().filter(file -> file.getName().equals(fileName)).collect(Collectors.toList());
    }

    public boolean saveFile(String fileName, byte[] fileContent) {
        File newFile = new File(filesRepo, fileName);
        try {
            Path path = Files.write(newFile.toPath(), fileContent, StandardOpenOption.CREATE);
            System.err.printf("File written successfully to: %s%n", path.toString());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void addFile(String filePath) throws IOException {
        File file = new File(filePath);
        byte[] fileContents = Files.readAllBytes(file.toPath());
        File newFile = new File(String.format("%s%s%s", filesRepo.getAbsolutePath(), File.separator, file.getName()));
        Files.write(newFile.toPath(), fileContents, StandardOpenOption.CREATE);
        fileList.add(newFile);
    }
}
