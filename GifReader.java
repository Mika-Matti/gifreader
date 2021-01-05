package Gifreader;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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

    //Contents of Image Descriptor 10 bytes
    private int imageSeparator; //1 byte, always 0x2C
    private int imageLeft; //2 bytes
    private int imageTop; //2 bytes
    private int imageWidth; //2 bytes
    private int imageHeight; //2 bytes
    private String imagePackedField; //1 byte [0][0][0][00][000]


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
        imageSeparator = bytes[ind]; //1 byte, always 0x2C
        ind++;
        imageLeft = read16(bytes, ind, 2); //2 bytes
        ind += 2;
        imageTop = read16(bytes, ind, 2); //2 bytes
        ind += 2;
        imageWidth = read16(bytes, ind, 2); //2 bytes
        ind += 2;
        imageHeight = read16(bytes, ind, 2); //2 bytes
        ind += 2;
        imagePackedField = Integer.toBinaryString(bytes[ind] & 255 | 256).substring(1); //Open this byte into 8 bits
        ind++;

        System.out.println("Image Separator = " + imageSeparator);
        System.out.println("imageLeft = " + imageLeft);
        System.out.printf("imageTop = " + imageTop);
        System.out.println("Image width = " + imageWidth);
        System.out.println("Image height = " + imageHeight);
        System.out.println("Image packed field = " + imagePackedField);
        System.out.println("CURRENT IND = " + ind);
        //END of Image Descriptor

        //Local color table would be here, if there was 1 in previous data in image packedfield 1 leftmost bit

        //START OF IMAGE DATA
        int LZWminCodeSize = bytes[ind]; //Value used to defcode compressed output codes mincode 2 would be 2-12 bits
        ind++;
        //Data subblocks
        int bytesToFollow = bytes[ind] & 0xff; //0x00-0xFF
        ind++;
        System.out.println("LZW minimum code size = " + LZWminCodeSize);
        System.out.println("Bytes of image data to follow = " + bytesToFollow + " bytes");

        String binaryStream = readStream(bytes, ind, bytesToFollow); //This comes out as 001100011... where values are read backwards = 4, 1, 6 etc
        ind += bytesToFollow;

        int length = LZWminCodeSize+1; //the initial binary length of value that will be read (3), increased whenever code = 2^(currentsize-1)
        int[] output = decode(binaryStream, length);
        //TODO take LWZ table sizes into consideration. and the fact that this only decodes one datablock. output = one datablock

        for(int i=0; i<output.length; i++) {
            int col = output[i];
            Color color = colors[col];
            String R = String.format("%03d", color.getRed());
            String G = String.format("%03d", color.getGreen());
            String B = String.format("%03d", color.getBlue());
            if((i+1)%imageWidth==0) {
                //System.out.println(output[i]);
                System.out.println("|" + R + "," + G + "," +B + "|");
            } else {
                //System.out.print(output[i]);
                System.out.print("|" + R + "," + G + "," +B + "|");
            }
        }


        //END OF IMAGE DATA
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

    private String readStream(byte[] arr, int start, int size) {
        int end = start+size;
        int aa = 0;
        StringBuilder hexString = new StringBuilder();
        String byteString = "";
        while(start < end) { //Read the bytes in byte order
            aa++;
            String nextByte = Integer.toBinaryString(arr[start] & 255 | 256).substring(1);
            byteString = nextByte + byteString;

            String nextHex = Integer.toHexString(arr[start] & 255 | 256).substring(1);
            hexString.append(nextHex);

            start++;
        }
        //flip bytestring
        String flippedString = "";
        char[] try1 = byteString.toCharArray();

        for (int i = try1.length - 1; i >= 0; i--)
            flippedString = flippedString + try1[i];

        System.out.println("reading " + flippedString.length() + " bit input: " + flippedString);
        System.out.println("reading " + hexString.length() + " bit input: " + hexString);


        return flippedString;
    }

    private int readVal(String curBin) {
        int curVal = 0;
        //Turn code into int
        int exp = 0;
        //read the string backwards and sum into int
        for(int i = 0; i < curBin.length(); i++) {
            int bit = curBin.charAt(i)-'0';
            if(bit==1) {
                double inc = Math.pow(2, exp); //add the bit to the sum
                curVal += inc;
            }
            exp++; //Increase the exponent
        }
        return curVal;
    }

    /**
     * This algorithm takes the datablock in binary data and decodes the compressed binary into pixel color values and returns them in an array
     * @param binaryStream
     * @param length
     * @return
     */
    private int[] decode(String binaryStream, int length) {
        int nextFreeInTable = 6;
        String[] table = new String[imageHeight*imageWidth];    //Table to store codes
        //TODO construct the table elsewhere and send as parameter along with nextFree from lwz table
        table[0] = "0,";
        table[1] = "1,";
        table[2] = "2,";
        table[3] = "3,";
        table[4] = "clear"; //clear
        table[5] = "end";

        String textput = "";
        int start = 0;

        String binCode = binaryStream.substring(start, start+length);
        start = start + length; //Update index
        int intCode = readVal(binCode); //#4

        System.out.print(binCode + " ");
        System.out.println(intCode);

        //read again before loop
        binCode = binaryStream.substring(start, start+length);
        start = start + length; //Update index
        intCode = readVal(binCode); //#1

        System.out.print(binCode + " ");
        System.out.println(intCode);
        System.out.println("output: " + table[intCode]);
        textput = textput+table[intCode];

        int prevCode = intCode;

        //Begin loop
        while(!table[intCode].equals("end")) {

            binCode = binaryStream.substring(start, (start+length)); //Read binary value from current index
            intCode = readVal(binCode); //turn binary value into int value

            System.out.print(binCode + " ");
            System.out.println(intCode);
            //is code in codetable?
            if(table[intCode] != null) { //Yes
                System.out.println("output: " + table[intCode]);
                textput = textput+table[intCode];
                String K = table[intCode].substring(0,2); //"0,"
                table[nextFreeInTable] = table[prevCode]+K;
                nextFreeInTable++; //update free slot
            } else { //No
                String K = table[prevCode].substring(0,2); //"0,"
                System.out.println("output:" + table[prevCode]+K);
                textput = textput+table[prevCode]+K;
                table[nextFreeInTable] = table[prevCode]+K;
                nextFreeInTable++;
            }

            //Update index
            start = start + length;
            prevCode = intCode;

            //If now read code equals to 2^currentsize-1
            if(nextFreeInTable-1 == (Math.pow(2, length)-1)) { //TODO instead of curVal you compare to the index of codetable(#7 for example)
                //Update current code length to +1
                System.out.println(nextFreeInTable-1 + " == " + "2^" + length + "-1 (" + (Math.pow(2, length)-1) + ")");
                length = length+1;
            }
        }

        int[] output = new int[imageHeight*imageWidth];
        String[] et = textput.split(",");

        for(int i=0; i<et.length-1; i++) {
            output[i] = Integer.parseInt(et[i]);
        }
        System.out.println(Arrays.toString(output));

        return output;
    }

}
