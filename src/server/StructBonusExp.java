package server;

public class StructBonusExp {

    public int incExpR;
    public int termStart;

    public boolean checkTerm(long now, long equipped) { //정령의펜던트
        int eHour = (int) ((now - equipped) / 60 / 60 / 1); //펜던트시간
        return eHour >= this.termStart;
    }
}
