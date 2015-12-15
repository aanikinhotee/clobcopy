package ee.icefire.clobcopy;

/**
 * Package: ee.icefire.clobcopy
 * User: anton
 * Date: 9/6/13
 * Time: 10:22 AM
 */
public class Template {

  String pdfPohi;
  String htmlPohi;
  Long allpId;
  String aldlKood;

  public String getFilenamePdf(){
    if(pdfPohi != null){
      return allpId + "_" + aldlKood + "_PDF.xsl";
    }
    return null;
  }


  public String getFilenameHtml(){
    if(htmlPohi != null){
      return allpId + "_" + aldlKood + "_HTML.xsl";
    }
    return null;
  }


  public Template(String pdfPohi, String htmlPohi, Long allpId, String aldlKood) {
    this.pdfPohi = pdfPohi;
    this.htmlPohi = htmlPohi;
    this.allpId = allpId;
    this.aldlKood = aldlKood;
  }


  public String getPdfPohi() {
    return pdfPohi;
  }

  public void setPdfPohi(String pdfPohi) {
    this.pdfPohi = pdfPohi;
  }

  public String getHtmlPohi() {
    return htmlPohi;
  }

  public void setHtmlPohi(String htmlPohi) {
    this.htmlPohi = htmlPohi;
  }

  public Long getAllpId() {
    return allpId;
  }

  public void setAllpId(Long allpId) {
    this.allpId = allpId;
  }

  public String getAldlKood() {
    return aldlKood;
  }

  public void setAldlKood(String aldlKood) {
    this.aldlKood = aldlKood;
  }
}
