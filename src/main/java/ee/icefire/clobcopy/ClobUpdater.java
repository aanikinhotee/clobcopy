package ee.icefire.clobcopy;

import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.lob.LobHandler;

import javax.sql.DataSource;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 10/3/12
 * Time: 11:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class ClobUpdater {


  public static String ClobToString(Clob body) throws SQLException {
    if(body != null){
      int offset = -1;
      int chunkSize = 1024;
      long bodyLength = body.length();
      if (chunkSize > bodyLength) {
        chunkSize = (int) bodyLength;
      }
      char buffer[] = new char[chunkSize];
      StringBuffer stringBuffer = new StringBuffer();
      Reader reader = body.getCharacterStream();

      try {
        while ((offset = reader.read(buffer)) != -1) {
          stringBuffer.append(buffer, 0, offset);
        }
      } catch (IOException e) {
        return null;
      }

      return stringBuffer.toString();
    }
    return null;
  }

  private static final ArrayList<char[]> REPLACEMENTS = new ArrayList<>();
  static {
    REPLACEMENTS.add(new char[] { (char) 213, (char) 336 });// LATIN CAPITAL LETTER O WITH CARON - Õ
    REPLACEMENTS.add(new char[] { (char) 245, (char) 337 });// LATIN SMALL LETTER O WITH CARON - õ
    REPLACEMENTS.add(new char[] { (char) 381, (char) 354 });// LATIN CAPITAL LETTER Z WITH CARON - Ž
    REPLACEMENTS.add(new char[] { (char) 382, (char) 355 });// LATIN SMALL LETTER Z WITH CARON - ž
    REPLACEMENTS.add(new char[] { (char) 352, (char) 272 });// LATIN CAPITAL LETTER S WITH CARON - Š
    REPLACEMENTS.add(new char[] { (char) 353, (char) 273 });// LATIN SMALL LETTER S WITH CARON - š
  }

  protected static String replaceCorrectCharset2MKR(String string) {
    if (string != null) {
      for (Object REPLACEMENT : REPLACEMENTS) {
        char[] rep = (char[]) REPLACEMENT;
        string = string.replace(rep[0], rep[1]);
      }
    }
    return string;
  }

  protected static String replaceMKR2CorrectCharset(String string) {
    if (string == null) {
      return "";
    }
    else if (string.trim().length() > 0) {
      for (Object REPLACEMENT : REPLACEMENTS) {
        char[] rep = (char[]) REPLACEMENT;
        string = string.replace(rep[1], rep[0]);
      }
    }
    return string;
  }


  public enum CheckResult{
    NEW, OLD
  }

  DataSource dataSource;

  LobHandler lobHandler;

  String encoding = "ANSI";       // baasi kodeering
  //String encoding = "UTF8";     // baasi kodeering

  public LobHandler getLobHandler() {
    return lobHandler;
  }

  public void setLobHandler(LobHandler lobHandler) {
    this.lobHandler = lobHandler;
  }

  public DataSource getDataSource() {
    return dataSource;
  }


  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  JdbcTemplate jdbcTemplate;

  public void init(){
    if(jdbcTemplate == null){
      jdbcTemplate = new JdbcTemplate();
      jdbcTemplate.setDataSource(getDataSource());
    }
  }

  private static Logger LOG = Logger.getLogger(ClobUpdater.class);

  public List<Template> exportAll(){

    init();

    List<Template> clobs = (List<Template>) jdbcTemplate.query("select allp_id, aldl_kood, " +
      "pdf_pohi, pohi from ma2.alg_dok_liik_pohi", new ResultSetExtractor<Object>() {
        @Override
        public Object extractData(ResultSet resultSet) throws SQLException, DataAccessException {
          List<Template> clobs1 = new ArrayList<Template>();
          while (resultSet.next()){
            clobs1.add(new Template(ClobUpdater.ClobToString(resultSet.getClob("pdf_pohi")),
              ClobUpdater.ClobToString(resultSet.getClob("pohi")),
              resultSet.getLong("allp_id"), resultSet.getString("aldl_kood")));
          }
          return clobs1;
        }
      });

    return clobs;
  }


  public void processClob(final String filedata, final String allpId
    , final String tyyp, final String aldlKood) throws FileNotFoundException, ClobUpdaterException {

    System.out.println("Start processing allp_id=" + allpId + " aldlKood=" + aldlKood + " tyyp=" + tyyp);
    LOG.info("Start processing allp_id=" + allpId + " aldlKood=" + aldlKood + " tyyp=" + tyyp);

    init();

    Long allpIdL = new Long(allpId);

    CheckResult result = checkTemplate(aldlKood, allpIdL);

    if(result.equals(CheckResult.NEW)){
      if(tyyp.equals("PDF")){
        LOG.info("Insert new PDF");
        insertPdf(allpIdL, filedata, aldlKood);
      } else if(tyyp.equals("HTML")){
        LOG.info("Insert new HTML");
        insertHtml(allpIdL, filedata, aldlKood);
      }
    } else if(result.equals(CheckResult.OLD)) {
      if(tyyp.equals("PDF")){
        LOG.info("Update PDF");
        updatePdf(allpIdL, filedata);
      } else if(tyyp.equals("HTML")){
        LOG.info("Update HTML");
        updateHtml(allpIdL, filedata);
      }
    }
    System.out.println("Processed");
  }

  public CheckResult checkTemplate(final String aldlKood, final Long allpId) throws ClobUpdaterException {
    String result = (String) jdbcTemplate.query("select aldl_kood from ma2.alg_dok_liik_pohi " +
      "WHERE allp_id = ?", new PreparedStatementSetter() {
        @Override
        public void setValues(PreparedStatement ps) throws SQLException {
          ps.setLong(1, allpId);
        }
        }, new ResultSetExtractor<Object>() {
          @Override
          public Object extractData(ResultSet resultSet) throws SQLException, DataAccessException {
            if(resultSet.next()){
              return resultSet.getString("aldl_kood");
            }
            return null;
          }
        });
    if(result == null){
      return CheckResult.NEW;
    } else if(result.equals(aldlKood)){
      return CheckResult.OLD;
    } else {
      throw new ClobUpdaterException("Id found but aldlKood is not the same");
    }
  }

  private void updatePdf(final Long allpId, final String filedata) {


    final String filedataConverted = optimizeTemplate(filedata);

    jdbcTemplate.update("UPDATE ma2.alg_dok_liik_pohi " +
      "SET pdf_pohi = ? " +
      ", muutm_aeg = ? " +
      ", muutm_sess_id = USER " +
      "WHERE allp_id = ?"
      , new PreparedStatementSetter() {
      public void setValues(PreparedStatement ps) throws SQLException {
        getLobHandler().getLobCreator().setClobAsString(ps, 1, filedataConverted);
        ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        ps.setLong(3, allpId);
      }
    }
    );
  }

  private void updateHtml(final Long allpId, final String filedata) {
    jdbcTemplate.update("UPDATE ma2.alg_dok_liik_pohi " +
      "SET pohi = ? " +
      ", muutm_aeg = ? " +
      ", muutm_sess_id = USER " +
      "WHERE allp_id = ?"
      , new PreparedStatementSetter() {
      public void setValues(PreparedStatement ps) throws SQLException {
        getLobHandler().getLobCreator().setClobAsString(ps, 1, filedata);
        ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        ps.setLong(3, allpId);
      }
    }
    );
  }


  private void insertPdf(final Long allpId, final String filedata, final String aldlKood) {

    final String filedataConverted = optimizeTemplate(filedata);

    jdbcTemplate.update("INSERT INTO ma2.alg_dok_liik_pohi " +
      "(allp_id, aldl_kood, keel, pdf_pohi, sisest_aeg, sisest_sess_id, sisest_isik_id" +
      ", muutm_aeg, muutm_sess_id, muutm_isik_id, keht_alg_kpv) " +
      "values( ?, ?, 'EE', ?, sysdate, USER, 2, sysdate, USER, 2, trunc(sysdate))"
      , new PreparedStatementSetter() {
      public void setValues(PreparedStatement ps) throws SQLException {
        ps.setLong(1, allpId);
        ps.setString(2, aldlKood);
        getLobHandler().getLobCreator().setClobAsString(ps, 3, filedataConverted);

      }
    }
    );
  }

  private String optimizeTemplate(String filedata) {
    final String filedataConverted;
    if("UTF8".equals(encoding)){
      filedataConverted = filedata;
    } else {
      filedataConverted = replaceCorrectCharset2MKR(filedata);
    }
    return filedataConverted;
  }

  private void insertHtml(final Long allpId, final String filedata, final String aldlKood) {
    jdbcTemplate.update("INSERT INTO ma2.alg_dok_liik_pohi " +
      "(allp_id, aldl_kood, keel, pohi, sisest_aeg, sisest_sess_id, sisest_isik_id" +
      ", muutm_aeg, muutm_sess_id, muutm_isik_id, keht_alg_kpv) " +
      "values( ?, ?, 'EE', ?, sysdate, USER, 2, sysdate, USER, 2, trunc(sysdate))"
      , new PreparedStatementSetter() {
      public void setValues(PreparedStatement ps) throws SQLException {
        ps.setLong(1, allpId);
        ps.setString(2, aldlKood);
        getLobHandler().getLobCreator().setClobAsString(ps, 3, filedata);
      }
    }
    );
  }
}
