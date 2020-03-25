import java.util.*;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

public class libraryExtract {
    private Mat wateredImg;
    private Mat RChannel;
    private Mat recoverImg;
    private String addiMsg ="";
    private String waterMsg="";
    private int peakPoint;
    private int zeroPoint;
    private int waterLen;

    public List<String> extract(String hidePath, String foldname){
        wateredImg = Imgcodecs.imread(hidePath);

        //取出R通道
        List<Mat> imgChannel = new ArrayList<Mat>();
        Core.split(wateredImg, imgChannel);
        RChannel = imgChannel.get(0);

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

        //提取峰值点、零值点、水印长度
        for (int i = 0; i < 3 * 8; i++){
            if (RPlain[i] % 2 == 0)
                addiMsg += "0";
            else
                addiMsg += "1";
        }
        String peakStr = addiMsg.substring(0,8);
        peakPoint = Integer.valueOf(peakStr,2);
        String zeroStr = addiMsg.substring(8,16);
        zeroPoint = Integer.valueOf(zeroStr,2);
        String lenStr = addiMsg.substring(16,24);
        waterLen = 8 * Integer.valueOf(lenStr,2);

        //提取水印信息
        if (zeroPoint > peakPoint){
            int cnt = 0;
            for (int i = 24; i < RPlain.length; i++){
                if (RPlain[i] == peakPoint){
                    waterMsg += "0";
                    cnt++;
                    if (cnt >= waterLen) break;
                }
                else if (RPlain[i] == peakPoint + 1){
                    waterMsg += "1";
                    cnt++;
                    if (cnt >= waterLen) break;
                }
            }
        }

        if (zeroPoint < peakPoint){
            int cnt = 0;
            for (int i = 24; i < RPlain.length; i++){
                if (RPlain[i] == peakPoint){
                    waterMsg += "0";
                    cnt++;
                    if (cnt >= waterLen) break;
                }
                else if (RPlain[i] == peakPoint - 1){
                    waterMsg += "1";
                    cnt++;
                    if (cnt >= waterLen) break;
                }
            }
        }

        //二进制解码为字符串
        byte[] resultByte = new byte[waterMsg.length()/8];
        for (int i = 0; i < resultByte.length; i++){
            String tmp = waterMsg.substring(i*8, i*8+8);
            resultByte[i] = Long.valueOf(tmp, 2).byteValue();
        }
        String result = new String(resultByte);

        //恢复原图像
        if (zeroPoint > peakPoint){
            for (int i = 24; i < RPlain.length; i++){
                if (RPlain[i] > peakPoint + 1 && RPlain[i] < zeroPoint)
                    RPlain[i]--;
                else if (RPlain[i] == peakPoint + 1)
                    RPlain[i]--;
            }
        }

        if (zeroPoint < peakPoint){
            for (int i = 24; i < RPlain.length; i++){
                if (RPlain[i] > zeroPoint + 1 && RPlain[i] < peakPoint)
                    RPlain[i]++;
                else if (RPlain[i] == peakPoint - 1)
                    RPlain[i]++;
            }
        }

        int[][] Rrecover = new int [wateredImg.rows()][wateredImg.cols()];
        int count = 0;
        for (int i = 0; i < Rrecover.length; i++){
            for (int j = 0; j < Rrecover[0].length; j++){
                Rrecover[i][j] = RPlain[count++];
            }
        }

        Mat RrecoverImg = RChannel.clone();
        for (int i = 0; i < Rrecover.length; i++){
            for (int j = 0; j < Rrecover[0].length; j++){
                RrecoverImg.put(i,j,Rrecover[i][j]);
            }
        }

        recoverImg = wateredImg.clone();
        List<Mat> imgChannelWater = new ArrayList<Mat>();
        imgChannelWater.add(RrecoverImg);
        imgChannelWater.add(imgChannel.get(1));
        imgChannelWater.add(imgChannel.get(2));
        Core.merge(imgChannelWater, recoverImg);

        Imgcodecs.imwrite(foldname, recoverImg);

        List<String> ans = new LinkedList<>();

        //返回值：水印信息+恢复出来的原始图像保存路径
        ans.add(result);
        ans.add(foldname);
//		List<String> res = (List<String>)list.get(1);
        return ans;
    }
}
