/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package provider;

import java.awt.Point;
import java.awt.image.BufferedImage;

public class MapleDataTool {

    public static String getString(MapleData data) {
        return ((String) data.getData());
    }

    public static String getString(MapleData data, String def) {
        if (data == null || data.getData() == null) {
            return def;
        } else if (data.getType() == MapleDataType.STRING || data.getData() instanceof String) {
            return ((String) data.getData());
        } else {
            return String.valueOf(getInt(data));
        }
    }

    public static String getString(String path, MapleData data) {
        return getString(data.getChildByPath(path));
    }

    public static String getString(String path, MapleData data, String def) {
        return getString(data == null || data.getChildByPath(path) == null ? null : data.getChildByPath(path), def);
    }

    public static double getDouble(MapleData data) {
        return ((Double) data.getData()).doubleValue();
    }

    public static float getFloat(MapleData data) {
        return ((Float) data.getData()).floatValue();
    }

    public static float getFloat(MapleData data, float def) {
        if (data == null || data.getData() == null) {
            return def;
        } else {
            return ((Float) data.getData()).floatValue();
        }
    }

    public static int getInt(MapleData data) {
        if (data == null || data.getData() == null) {
            return 0;
        }

        Object value = data.getData();

        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                System.err.println("NumberFormatException: Cannot convert String to Integer. Value: " + value);
                return 0;
            }
        } else if (value instanceof Short) {
            return ((Short) value).intValue();
        } else {
            throw new ClassCastException("Expected Integer but found " + value.getClass().getName());
        }
    }

    public static int getInt(MapleData data, int def) {
        if (data == null || data.getData() == null) {
            return def;
        }

        Object value = data.getData();
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                System.err.println("NumberFormatException: Cannot convert String to Integer. Value: " + value);
                return def;
            }
        } else if (value instanceof Short) {
            return ((Short) value).intValue();
        } else {
            return def;
        }
    }

    public static int getInt(String path, MapleData data) {
        return getInt(data.getChildByPath(path));
    }

    public static int getIntConvert(MapleData data) {
        if (data == null || data.getData() == null) {
            return 0;
        }

        if (data.getType() == MapleDataType.STRING) {
            try {
                String value = getString(data);
                if (value != null) {
                    return Integer.parseInt(value);
                }
            } catch (NumberFormatException e) {
                System.err.println("NumberFormatException: Cannot convert String to Integer.");
            }
            return 0;
        } else {
            return getInt(data);
        }
    }

    public static int getIntConvert(String path, MapleData data) {
        MapleData d = (data != null) ? data.getChildByPath(path) : null;
        if (d == null || d.getData() == null) {
            return 0;
        }

        return getIntConvert(d);
    }

    public static int getInt(String path, MapleData data, int def) {
        if (data == null) {
            return def;
        }
        return getInt(data.getChildByPath(path), def);
    }

    public static int getIntConvert(String path, MapleData data, int def) {
        if (data == null) {
            return def;
        }
        return getIntConvert(data.getChildByPath(path), def);
    }

    public static int getIntConvert(MapleData d, int def) {
        if (d == null) {
            return def;
        }
        if (d.getType() == MapleDataType.STRING) {
            String dd = getString(d);
            if (dd.endsWith("%")) {
                dd = dd.substring(0, dd.length() - 1);
            }
            try {
                return Integer.parseInt(dd);
            } catch (NumberFormatException nfe) {
                return def;
            }
        } else {
            return getInt(d, def);
        }
    }

    public static BufferedImage getImage(MapleData data) {
        return ((MapleCanvas) data.getData()).getImage();
    }

    public static Point getPoint(MapleData data) {
        return ((Point) data.getData());
    }

    public static Point getPoint(String path, MapleData data) {
        return getPoint(data.getChildByPath(path));
    }

    public static Point getPoint(String path, MapleData data, Point def) {
        final MapleData pointData = data.getChildByPath(path);
        if (pointData == null) {
            return def;
        }
        return getPoint(pointData);
    }

    public static String getFullDataPath(MapleData data) {
        String path = "";
        MapleDataEntity myData = data;
        while (myData != null) {
            path = myData.getName() + "/" + path;
            myData = myData.getParent();
        }
        return path.substring(0, path.length() - 1);
    }
    
    public static long getLongConvert(String path, MapleData data) { 
        MapleData d = data.getChildByPath(path); 
        if (d == null) { 
            return 0L; 
        } 
        if (d.getType() == MapleDataType.STRING) { 
            if (getString(d).equals("??????")) { 
                return 1L; 
            } 
            return Long.parseLong(getString(d)); 
        } 
        return getLong(d); 
    } 

    public static long getLong(MapleData data) { 
        return ((Long) data.getData()).longValue(); 
    }
}
