import java.util.*;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;


public class libraryEmbed {
    private String imagePath;
    private String waterPath;
    private Mat img;
    private Mat imgWater;
    private byte[] word = null;
    private int peakPoint;
    private int peakNum;
    private int zeroPoint;

    libraryEmbed(String imagePath, String waterPath){
        this.imagePath = imagePath;
        this.waterPath = waterPath;
    }

    public String hide(String foldname, String saveAs) throws Exception {
        img = Imgcodecs.imread(imagePath);
        String hidePath = foldname.concat("\\").concat(saveAs);

        try{
            word  = waterPath.getBytes("gbk");
        }
        catch(Exception e){
            e.printStackTrace();
        }

        StringBuffer water1 = new StringBuffer();
        for (int i = 0; i < word.length; i++) {
            if (Long.toString(word[i] & 0xff, 2).length() == 6)
                water1.append("00" + Long.toString(word[i] & 0xff, 2) );
            else if(Long.toString(word[i] & 0xff, 2).length() == 7)
                water1.append("0" + Long.toString(word[i] & 0xff, 2) );
        }
        String water = water1.toString().substring(0, water1.length());

//        byte[] result = new byte[water.length()/8];
//        for (int i = 0; i < result.length; i++){
//            String tmp = water.substring(i*8, i*8+8);
//            result[i] = Long.valueOf(tmp, 2).byteValue();
//        }
//        String str = new String(result);

        //在空域的R通道嵌入
        List<Mat> imgChannel = new ArrayList<Mat>();
        Core.split(img, imgChannel);
        Mat RChannel = imgChannel.get(0);

        int[][] RC = new int[RChannel.rows()][RChannel.cols()];
        for (int i = 0; i < RChannel.rows(); i++){
            for (int j = 0; j < RChannel.cols(); j++){
                RC[i][j]= (int)RChannel.get(i, j)[0];
            }
        }
        int[] RPlain = new int [RC.length*RC[0].length];
        int index = 0;
        for (int i = 0; i < RC.length; i++) {
            for (int j = 0; j < RC[0].length; j++) {
                RPlain[index++] = RC[i][j];
            }
        }

        //统计直方图
        int[] stat = new int[256];
        for (int i = 0; i < RPlain.length; i++){
            stat[RPlain[i]]++;
        }

        //确定峰值点
        peakNum = getMaxIndex(stat)[0];      //峰值，即嵌入容量
        peakPoint = getMaxIndex(stat)[1];        //峰值索引
        if (peakPoint == 255){               //若255处为峰值则取次峰值
            peakNum = getMaxIndex(stat)[2];
            peakPoint = getMaxIndex(stat)[3];
        }

        System.out.println("Max Embed: " + (int)Math.floor(peakNum / 8) + " character(s)");
        //确定零值点
        boolean zeroTag = false;
        for (int i = peakPoint; i < 256; i++){
            if (stat[i] == 0){
                zeroPoint = i;
                zeroTag = true;
                break;
            }
        }
        if (!zeroTag){
            for (int i = peakPoint; i >=0 ; i--){
                if (stat[i] == 0){
                    zeroPoint = i;
                    zeroTag = true;
                    break;
                }
            }
        }
        if (!zeroTag){
            throw new Exception("The original image cannot be embedded!!");
        }

        if (peakNum < water.length()){
            throw new Exception("The watermark is too long, please shorter!!");
        }

        //嵌入附加信息
        int peakTmp = 1 << 8 | peakPoint;
        String peakBin = Integer.toBinaryString(peakTmp).substring(1);
        int zeroTmp = 1 << 8 | zeroPoint;
        String zeroBin = Integer.toBinaryString(zeroTmp).substring(1);
        int waterTmp = 1 << 8 | water.length()/8;
        String waterBin = Integer.toBinaryString(waterTmp).substring(1);

        String addiMsg = peakBin.concat(zeroBin).concat(waterBin);
        for (int i = 0; i < addiMsg.length(); i++){
            if (addiMsg.charAt(i)=='0')
                RPlain[i] = RPlain[i] - RPlain[i] % 2;
            else if (addiMsg.charAt(i)=='1')
                RPlain[i] = RPlain[i] - RPlain[i] % 2 + 1;
        }

        //嵌入水印信息
        if (zeroPoint > peakPoint){
            int cnt = 0;
            //直方图平移
            for (int i = addiMsg.length(); i < RPlain.length; i++){
                if (RPlain[i] > peakPoint && RPlain[i] < zeroPoint)
                    RPlain[i]++;
            }
            //嵌入
            for (int i = addiMsg.length(); i < RPlain.length; i++){
                if (RPlain[i] == peakPoint){
                    if (water.charAt(cnt) == '0'){
                        cnt++;
                        if (cnt >= water.length()) break;
                    }
                    else if (water.charAt(cnt) == '1'){
                        RPlain[i] = RPlain[i] + 1;
                        cnt++;
                        if (cnt >= water.length()) break;
                    }
                }
            }
        }

        if (zeroPoint < peakPoint){
            int cnt = 0;
            //直方图平移
            for (int i = addiMsg.length(); i < RPlain.length; i++){
                if (RPlain[i] > zeroPoint && RPlain[i] < peakPoint)
                    RPlain[i]--;
            }
            //嵌入
            for (int i = addiMsg.length(); i < RPlain.length; i++){
                if (RPlain[i] == peakPoint){
                    if (water.charAt(cnt) == '0'){
                        cnt++;
                        if (cnt >= water.length()) break;
                    }
                    else if (water.charAt(cnt) == '1'){
                        RPlain[i] = RPlain[i] - 1;
                        cnt++;
                        if (cnt >= water.length()) break;
                    }
                }
            }
        }

        int[][] Rwater = new int [img.rows()][img.cols()];
        int count = 0;
        for (int i = 0; i < Rwater.length; i++){
            for (int j = 0; j < Rwater[0].length; j++){
                Rwater[i][j] = RPlain[count++];
            }
        }
        Mat RImgWater = RChannel.clone();
        for (int i = 0; i < Rwater.length; i++){
            for (int j = 0; j < Rwater[0].length; j++){
                RImgWater.put(i,j,Rwater[i][j]);
            }
        }

        imgWater = img.clone();
        List<Mat> imgChannelWater = new ArrayList<Mat>();
        imgChannelWater.add(RImgWater);
        imgChannelWater.add(imgChannel.get(1));
        imgChannelWater.add(imgChannel.get(2));
        Core.merge(imgChannelWater, imgWater);

        Imgcodecs.imwrite(hidePath, imgWater);
        return hidePath;
    }


    public static int[] getMaxIndex(int[] arr) {
        if(arr==null||arr.length==0){
            return null;
        }
        int maxIndex=0;
        int secondMaxIndex=0;
        int[] arrnew=new int[4];
        for(int i =0;i<arr.length-1;i++){
            if(arr[maxIndex]<arr[i+1]){
                secondMaxIndex=maxIndex;
                maxIndex=i+1;
            }
            else{
                if (arr[i+1]>arr[secondMaxIndex]){
                    secondMaxIndex=i+1;
                }
            }
        }
        arrnew[0]=arr[maxIndex];
        arrnew[1]=maxIndex;
        arrnew[2]=arr[secondMaxIndex];
        arrnew[3]=secondMaxIndex;
        return arrnew;
    }
}
