import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static Connection con = null;
    public static long last_insert_id_sln = 0;
    public static long last_insert_id_proj = 0;
    public static long last_insert_id_dependence = 0;


    public static void main(String[] args) throws Exception{
        System.out.println("Begin load driver");
        Class.forName("com.mysql.cj.jdbc.Driver");
        //con = DriverManager.getConnection("jdbc:mysql://"+args[1] + ":3306/"+args[2],args[3], args[4]);
        System.out.println("Begin connect");
        con = DriverManager.getConnection("jdbc:mysql://localhost/TreSet?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&autoReconnect=true&useSSL=false", "user", "user");
        System.out.println(con);
        System.out.println("Begin look in the directories");
        //getListFiles(args[0]);
        getListFiles("/home/uroot/42labs");
   }

    public static void addSLN(String sln) throws SQLException {
        PreparedStatement stmt = con.prepareStatement("INSERT INTO FilesSLN(slnname) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS);
        stmt.setString(1, sln);

        long affectedRows = stmt.executeUpdate();

        if (affectedRows == 0) {
            throw new SQLException("Создание sln не удалось.");
        }

        ResultSet genKeys = stmt.getGeneratedKeys();
        if (genKeys.next()) {
            last_insert_id_sln = genKeys.getLong(1);
        }
    }

    public static void addCproj(String cproj) throws Exception{
        PreparedStatement stmt = con.prepareStatement("INSERT INTO FilesCsproj(name, id_sln) VALUES (?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
        stmt.setString(1, cproj);
        stmt.setLong(2, last_insert_id_sln);

        long affectedRows = stmt.executeUpdate();

        if (affectedRows == 0) {
            throw new SQLException("Создание sln не удалось.");
        }

        ResultSet genKeys = stmt.getGeneratedKeys();
        if (genKeys.next()) {
            last_insert_id_proj = genKeys.getLong(1);
        }
    }

    public static void addDependency(String dependency) throws Exception{
        PreparedStatement stmt = con.prepareStatement("INSERT INTO projDependencies(name, id_proj) VALUES (?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
        stmt.setString(1, dependency);
        stmt.setLong(2, last_insert_id_proj);

        long affectedRows = stmt.executeUpdate();

        if (affectedRows == 0) {
            throw new SQLException("Создание sln не удалось.");
        }

        ResultSet genKeys = stmt.getGeneratedKeys();
        if (genKeys.next()) {
            last_insert_id_dependence = genKeys.getLong(1);
        }
    }


    public static void getListFiles(String dir) throws Exception {
        File folder = new File(dir);

        Set<File> filesCSPROJ = new TreeSet<>();
        Set<String> dllList = new TreeSet<>();
        System.out.println(folder);
        for (File f : folder.listFiles()){

            if (f.isFile()){
                if (f.getName().endsWith(".sln")){

                    System.out.println("+++  " + f);

                    String currentPath = getPathFile(f);
                    addSLN(f.getAbsolutePath());

                    String content = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath())), StandardCharsets.UTF_8);
                    Pattern p = Pattern.compile("[\\w\\d\\\\_\\-\\s0-9\\.]+\\.*proj");
                    Matcher m = p.matcher(content);
                    while (m.find()){
                        String filepath = (currentPath + File.separator + content.substring(m.start(), m.end())).replaceAll(" ", "");
                        filesCSPROJ.add(new File(filepath));
                    }
                    filesCSPROJ.forEach(strpath->{
                        if (strpath.toString().endsWith(".csproj")){
                            System.out.println(strpath);
                            try {
                                addCproj(strpath.getAbsolutePath());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            try {
                                String sContent = new String(Files.readAllBytes(Paths.get(strpath.getAbsolutePath())),StandardCharsets.UTF_8);
                                Pattern p1 = Pattern.compile("<Reference Include=\"(\\w.*)\"");
                                Matcher m1 = p1.matcher(sContent);
                                while (m1.find()){
                                    String tmpcontent = sContent.substring(m1.start(), m1.end());
                                    Pattern p2 = Pattern.compile("<Reference Include=\"");
                                    Matcher m2 = p2.matcher(tmpcontent);
                                    while (m2.find()) {
                                        String tmp = tmpcontent.substring(m2.end(), tmpcontent.length());
                                        try {
                                            addDependency(tmp);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            try {
                                addCproj(strpath.getAbsolutePath());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
            else if (f.isDirectory()){
                    getListFiles(f.getAbsolutePath());
            }
        }

    }

    public static String getPathFile(File f){
        String absolutePath = f.getAbsolutePath();
        return absolutePath.substring(0,absolutePath.lastIndexOf(File.separator)).replaceAll(" ", "");
    }
}
