package me.maeroso;

import java.io.File;

public class FileBytesTuple {
    File fileObject;
    byte[] fileContent;

    FileBytesTuple(File fileObject, byte[] fileContent) {
        this.fileObject = fileObject;
        this.fileContent = fileContent;
    }
}
