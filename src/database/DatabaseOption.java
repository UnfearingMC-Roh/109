/*
 * Copyright (C) 2013 Nemesis Maple Story Online Server Program

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package database;

import server.Start;

/**
 *
 * @author Eternal
 */
public class DatabaseOption {
//    

    public static final String MySQLURL = Start.SQL_URL;
    public static final String MySQLUSER = Start.SQL_USER;
    public static final String MySQLPASS = Start.SQL_PASSWORD;

    public static int MySQLMINCONNECTION = 10;
    public static int MySQLMAXCONNECTION = 50000;

}
