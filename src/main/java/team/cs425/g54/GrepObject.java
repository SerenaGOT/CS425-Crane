package team.cs425.g54;

import java.io.Serializable;

public class GrepObject  implements Serializable {
    public int vmNum = 0;
    public String totalline = "";
    public int index = 0;
    public String grepResult = "";
    public GrepObject(String totalline, int index, String grepResult,int vmNum){
        this.vmNum = vmNum;
        this.totalline = totalline;
        this.index = index;
        this.grepResult = grepResult;
    }
}
