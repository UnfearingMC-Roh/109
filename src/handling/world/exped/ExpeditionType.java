package handling.world.exped;

public enum ExpeditionType {
    Easy_Balrog(6, 2000, 50, 70),
    Normal_Balrog(15, 2001, 50, 200),
    Zakum(30, 2002, 50, 200),
    Horntail(30, 2003, 80, 200),
    Pink_Bean(30, 2004, 140, 200),
    Chaos_Zakum(30, 2005, 100, 200),
    ChaosHT(30, 2006, 110, 200),
    CWKPQ(30, 2007, 90, 200),
    Von_Leon(30, 2008, 120, 200),
    Cygnus(18, 2009, 170, 200);

    public int maxMembers, maxParty, exped, minLevel, maxLevel, lastParty;

    private ExpeditionType(int maxMembers, int exped, int minLevel, int maxLevel) {
        this.maxMembers = maxMembers;
        this.exped = exped;
        //this.maxParty = (maxMembers / 2) + (maxMembers % 2 > 0 ? 1 : 0);
        this.maxParty = (maxMembers / 6) + (maxMembers % 6 > 0 ? 1 : 0);
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.lastParty = maxMembers % 6;
    }

    public static ExpeditionType getById(int id) {
        for (ExpeditionType pst : ExpeditionType.values()) {
            if (pst.exped == id) {
                return pst;
            }
        }
        return null;
    }

    public String getName() {
        switch (exped) {
            case 2000:
                return "이지발록 원정대";
            case 2001:
                return "노말발록 원정대";
            case 2002:
                return "자쿰 원정대";
            case 2003:
                return "혼테일 원정대";
            case 2004:
                return "핑크빈 원정대";
            default:
                return "알 수 없음";
        }
    }

    public int getId() {
        return exped;
    }
}
