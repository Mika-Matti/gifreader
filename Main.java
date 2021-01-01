import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.awt.Color;
import java.util.Arrays;

import javafx.stage.Stage;

public class Main {

//    @Override
//    public void start(Stage primaryStage) throws Exception{
//        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
//        primaryStage.setTitle("Gif reader");
//        primaryStage.setScene(new Scene(root, 300, 275));
//        primaryStage.show();
//    }

    public static void main(String[] args) {
        //launch(args);
        String fileName = "src/sample_1.gif";
        File file = new File(fileName);
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        int ind = 0; //Current index of byte

        //Structure of file in bytes
        int headerSize = 6;
        int LSDSize = 7; //logicalScreenDescriptorSize
        int dimSize = 2; //Amount of bytes per canvas dimension

        System.out.println("length of file is " + size + " bytes");

        try { //Read file into bytearray
            bytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //START of header first print header into string
        StringBuilder header = new StringBuilder();
        while (ind < headerSize) {
            char next = (char) bytes[ind]; //read byte into ASCII code
            header.append(next);
            ind++;
        }
        System.out.println("File header: " + header);
        //END of header

        //START of logical screen descriptor (Canvas Width 2, Canvas Height 2, Packedfield 1, BG Color Index 1, pixel aspect ratio 1)
        int width = read16(bytes, ind, dimSize);
        ind += dimSize; //Update index
        int height = read16(bytes, ind, dimSize);
        ind += dimSize; //Update index
        System.out.println("Width: " + width + " and height: " + height); //At this point we have read 4 out of 7 bytes of LSD

        String packedField = Integer.toBinaryString(bytes[ind] & 255 | 256).substring(1); //Open this byte into 8 bits
        ind++; //Move indicator
        System.out.println("Packedfield: " + packedField);

        int BGColorIndex = bytes[ind];
        ind++;
        int pixelAspectRatio = bytes[ind];
        ind++;
        System.out.println("Background Color Index = " + BGColorIndex + " and Pixel Aspect Ratio = " + pixelAspectRatio);
        //End of Screen descriptor, index should be at header+lsd

        //START of global color table In the start we open up some values from the packedfield
        int colorTableExist = packedField.charAt(0)-'0'; //either 1 or 2
        int colorResolution = (colorTableExist == 1 ? Integer.parseInt(packedField.substring(1, 4), 2) : 0); //If previous is one then read this resolution
        int globalColorTableSize = (colorTableExist == 1 ? (int) Math.pow(2, (Integer.parseInt(packedField.substring(5, 8), 2)+1)) : 0); //only important if color table exists

        System.out.println("Color resolution bits/pixel = " + colorResolution); //001 would be 2 pixels, 1 and 0 etc...
        System.out.println("Global color table size, if table exists = " + globalColorTableSize);
        int globalColorTableLength = (3*globalColorTableSize); //Bytes in global color table, length of this in bytes is 3*2^(N+1), where N = value of color depth field
        //The table is 3*2^(N+1) because every color is rgb (255, 255, 255) We have to read three bytes at a time from the color table to get a full color e.g #FFFFFF
        System.out.println("Actual amount of bytes in global color table = " + globalColorTableLength);

        System.out.println("CURRENT IND = " + ind);

        //Read colors into array
        Color[] colors = new Color[globalColorTableSize];
        for(int i=0; i < globalColorTableSize; i++) {
            int[] RGB = new int[3];
            for(int a=0; a < RGB.length; a++) {
                RGB[a] = bytes[ind] & 0xff; //This has to be read as unsigned byte to get 255 instead of -1
                ind++;
            }
            System.out.println(Arrays.toString(RGB));
            colors[i] = new Color(RGB[0], RGB[1], RGB[2]);
        }
        System.out.println(Arrays.toString(colors));
        System.out.println("CURRENT IND " + ind);


        //TODO read next parts after 24 bits starting from 25th bit. Check if there is still stuff to read in global color table
    }

    //END of global color table
    public static int read16(byte[] arr, int start, int size) {
        int end = start+size-1;
        StringBuilder byteString = new StringBuilder();
        while(end >= start) { //Read the bytes backwards from end to start
            String nextByte = Integer.toBinaryString(arr[end] & 255 | 256).substring(1);
            byteString.append(nextByte);
            end--;
        }
        System.out.println("reading " + byteString.length() + " bit word: " + byteString);
        double val = 0;
        int exp = 0;
        //read the string backwards and sum into int
        for(int i = byteString.length() - 1; i >= 0; i--) {
            int bit = byteString.charAt(i)-'0';
            if(bit==1) {
                double inc = Math.pow(2, exp); //add the bit to the sum
                val += inc;
            }
            exp++; //Increase the exponent
        }
        System.out.println("Value of binary is " + val);
        return (int) val;
    }
}
