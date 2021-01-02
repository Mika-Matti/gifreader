package Gifreader;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public class GifReader {
    //Attributes
    private int size;   //File bytesize
    private byte[] bytes;   //array of the bytes in the file
    private int ind;    //Index pointing to current byte for reading

    //Structure of file in bytelengths
    private int headerSize; //Header bytelength
    private int LSDSize;    //LogicalScreenDescriptor bytelength
    private int dimSize;    //Dimension bytelength inside LSD

    //Contents of header
    private StringBuilder header; //Content of header in ASCII format 6 bytes 3 for signature 3 for version

    //Contents of LogicalScreenDescriptor 7 bytes
    private int width;  //Canvas width 2 bytes
    private int height; //Canvas height 2 bytes
    private String packedField; //8 bit long word of properties 1 byte
    private int BGColorIndex; //BackgroundColorIndex  1 byte
    private int pixelAspectRatio; //Pixel aspect ratio 1 byte

    //Contents of global color table, from packed field
    private int colorTableExist; //either 1 or 0. First bit of packedfield
    private int colorResolution; //If previous is one then read this resolution. next 3 bits from packedfield e.g 001 = 2bits/pixel
    private int sortflag; //Either 1 or 0. Fifth bit of packedfield
    private int globalColorTableSize; //only important if color table exists. Value N is last 3 bits of packedfield and this value is 2^(N+1) bytes
    private int globalColorTableLength; //Bytes in global color table, length of this in bytes is 3*2^(N+1), where N = value of color depth field
    //The table is 3*2^(N+1) because every color is rgb (255, 255, 255) We have to read three bytes at a time from the color table to get a full c
    private Color[] colors; //Will hold the built out color table and will include Color objects with values rgb(255, 255, 255)

    //Contents of Graphics Color Extension 8 bytes used to specify transparency settings and control animations
    private int extIntroducer; //Extension introducer 1 byte always 0x21
    private int graphicsControlLabel; //Graphics Control Label 1 byte always 0xF9
    private int blockSize; //Total blocksize in bytes 1 byte
    private String graphPackedField; //Packedfield of Graphics Color extension 1 byte (holding [000][000][0][0])
    private int delayTime; //Two bytes
    private int transparentColorIndex; //1 byte
    private int blockTerminator; //1 byte, always 0x00


    public GifReader(File file) {
        size = (int) file.length(); //How many bytes the file is
        bytes = new byte[size];
        ind = 0; //Current index of byte

        //Structure of file in bytes
        headerSize = 6;
        LSDSize = 7; //logicalScreenDescriptorSize
        dimSize = 2; //Amount of bytes per canvas dimension
        System.out.println("length of file is " + size + " bytes");

        try { //Read file contents byte by byte into bytearray
            bytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        readContents(); //Start going through the byte segments of the file
    }

    private void readContents() {
        //TODO store values into arrays of attributes instead of invidual attributes, then loop over byte array

        //START of header first print header into string
        header = new StringBuilder();
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

        packedField = Integer.toBinaryString(bytes[ind] & 255 | 256).substring(1); //Open this byte into 8 bits
        ind++; //Move indicator
        System.out.println("Packedfield: " + packedField);

        BGColorIndex = bytes[ind];
        ind++;
        pixelAspectRatio = bytes[ind];
        ind++;
        System.out.println("Background Color Index = " + BGColorIndex + " and Pixel Aspect Ratio = " + pixelAspectRatio);
        //END of Screen descriptor, index should be at header+lsd

        //START of global color table In the start we open up some values from the packedfield
        colorTableExist = packedField.charAt(0)-'0'; //either 1 or 0
        colorResolution = (colorTableExist == 1 ? Integer.parseInt(packedField.substring(1, 4), 2) : 0); //If previous is one then read this resolution
        globalColorTableSize = (colorTableExist == 1 ? (int) Math.pow(2, (Integer.parseInt(packedField.substring(5, 8), 2)+1)) : 0); //only important if color table exists
        globalColorTableLength = (3*globalColorTableSize); //Bytes in global color table, length of this in bytes is 3*2^(N+1), where N = value of color depth field
        //The table is 3*2^(N+1) because every color is rgb (255, 255, 255) We have to read three bytes at a time from the color table to get a full color e.g #FFFFFF

        System.out.println("Color resolution bits/pixel = " + colorResolution); //001 would be 2 pixels, 1 and 0 etc...
        System.out.println("Global color table size, if table exists = " + globalColorTableSize);
        System.out.println("Actual amount of bytes in global color table = " + globalColorTableLength);
        System.out.println("CURRENT IND = " + ind);

        //Read colors into array
        colors = new Color[globalColorTableSize];
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
        //END of global color table

        //START of Graphics Control Extension 8 bytes
        extIntroducer = bytes[ind];
        ind++;
        graphicsControlLabel = bytes[ind] & 0xff; //This has to be read as unsigned byte to get 249 instead of -7
        ind++;
        blockSize = bytes[ind];
        ind++;
        graphPackedField = Integer.toBinaryString(bytes[ind] & 255 | 256).substring(1); //Open this byte into 8 bits
        ind++;
        delayTime = read16(bytes, ind, 2);; //This is two bytes of unsigned bits
        ind +=2;
        transparentColorIndex = bytes[ind];
        ind++;
        blockTerminator = bytes[ind];
        ind++;

        System.out.println("Extension introducer = " + extIntroducer);
        System.out.println("Graphics Control label = " + graphicsControlLabel);
        System.out.println("Block size = " + blockSize + " bytes");
        System.out.println("GPackedField = " + graphPackedField);
        System.out.println("Delay time = " + delayTime);
        System.out.println("Transparent Color Index = " + transparentColorIndex);
        System.out.println("Block terminator = " + blockTerminator);
        System.out.println("CURRENT IND = " + ind);
        //END of Graphics Control Extension

        //START of Image Descriptor 10 bytes

        //END of Image Descriptor
    }

    private int read16(byte[] arr, int start, int size) {
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
