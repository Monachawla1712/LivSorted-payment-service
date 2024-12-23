package com.sorted.rest.services.payment.utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ServerException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.services.payment.beans.LedgerDataBean;
import com.sorted.rest.services.payment.beans.LedgerTxnBean;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class LedgerPDFGenerator extends PdfPageEventHelper {

	private AppLogger _LOGGER = LoggingManager.getLogger(LedgerPDFGenerator.class);

	private final static Font COURIER_SMALL_FOOTER = new Font(Font.FontFamily.COURIER, 8, Font.BOLD);

	private final static Font DOCUMENT_HEADER = new Font(Font.FontFamily.COURIER, 10, Font.BOLD);

	private final static Font HEADER_CONTENT_FONT = new Font(Font.FontFamily.COURIER, 7, Font.BOLD);

	private final static Font TABLE_CONTENT_FONT = new Font(Font.FontFamily.COURIER, 7, Font.NORMAL, BaseColor.BLUE);

	public void generatePdfReport(LedgerDataBean ledgerDataBean) {
		_LOGGER.info(String.format("generatePdfReport:: Ledger Data : %s", ledgerDataBean));
		Document document = new Document();
		try {
			String directory = System.getProperty("user.dir");
			String ledgerFileName = ledgerDataBean.getFileName();
			PdfWriter.getInstance(document, new FileOutputStream(getPdfName(directory, ledgerFileName)));
			document.open();
			addLogo(document);
			addDocTitle(document, ledgerDataBean);
			addWalletStatement(ledgerDataBean, document);
			document.close();
		} catch (DocumentException | IOException e) {
			_LOGGER.error("Error while generating Ledger Report", e);
			throw new ServerException(new ErrorBean(Errors.UPDATE_FAILED, "Error while generating Ledger Report", "Ledger"));
		}
	}

	private void addWalletStatement(LedgerDataBean ledgerDataBean, Document document) throws DocumentException, IOException {
		int noOfColumns = 7;
		List<String> columnNames = new ArrayList<>();
		columnNames.add("Transaction Date");
		columnNames.add("Transaction Description");
		columnNames.add("Transaction Detail");
		columnNames.add("Order Date");
		columnNames.add("Debit");
		columnNames.add("Credit");
		columnNames.add("Running Balance");
		addDefaultTableFormatAndTxns(document, noOfColumns, columnNames, ledgerDataBean);
	}

	private void addLogo(Document document) throws DocumentException {
		PdfPTable table = new PdfPTable(1);
		table.setWidthPercentage(100);
		table.getDefaultCell().setPadding(5);
		table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
		table.getDefaultCell().setVerticalAlignment(Element.ALIGN_CENTER);

		Paragraph p = new Paragraph();
		p.setFont(DOCUMENT_HEADER);
		p.add("Ledger");
		PdfPCell cell = new PdfPCell(p);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_CENTER);
		cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
		table.addCell(cell);
		p = new Paragraph();
		p.setFont(DOCUMENT_HEADER);
		p.add("BCFD Technologies Private limited");
		cell = new PdfPCell(p);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_CENTER);
		cell.setBackgroundColor(new BaseColor(175, 203, 107));
		table.addCell(cell);
		document.add(table);
	}

	private void addDocTitle(Document document, LedgerDataBean ledgerDataBean) throws DocumentException {
		PdfPTable outerTable = new PdfPTable(1);
		PdfPCell cell;
		outerTable.setWidthPercentage(50);
		outerTable.getDefaultCell().setBorder(0);
		outerTable.getDefaultCell().setPadding(5);
		outerTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
		outerTable.getDefaultCell().setVerticalAlignment(Element.ALIGN_LEFT);
		orderDetails(outerTable, ledgerDataBean);
		cell = new PdfPCell();
		cell.setColspan(14);
		outerTable.addCell(cell);
		document.add(outerTable);
	}

	private void orderDetails(PdfPTable table, LedgerDataBean ledgerDataBean) {
		PdfPTable dynamicTable = new PdfPTable(2);
		dynamicTable.getDefaultCell().setBorder(0);
		dynamicTable.addCell(new Paragraph("Store Id # :", TABLE_CONTENT_FONT));
		dynamicTable.addCell(new Paragraph(ledgerDataBean.getStoreId(), TABLE_CONTENT_FONT));
		dynamicTable.addCell(new Paragraph("Store Name # :", TABLE_CONTENT_FONT));
		dynamicTable.addCell(new Paragraph(ledgerDataBean.getStoreName(), TABLE_CONTENT_FONT));
		dynamicTable.addCell(new Paragraph("From Date :", TABLE_CONTENT_FONT));
		dynamicTable.addCell(new Paragraph(ledgerDataBean.getFromDate(), TABLE_CONTENT_FONT));
		dynamicTable.addCell(new Paragraph("To Date :", TABLE_CONTENT_FONT));
		dynamicTable.addCell(new Paragraph(ledgerDataBean.getToDate(), TABLE_CONTENT_FONT));
		table.addCell(dynamicTable);
	}

	private void addDefaultTableFormatAndTxns(Document document, int noOfColumns, List<String> columnNames, LedgerDataBean ledgerDataBean)
			throws DocumentException, IOException {
		Paragraph paragraph = new Paragraph();
		leaveEmptyLine(paragraph, 3);
		paragraph.setFont(COURIER_SMALL_FOOTER);
		leaveEmptyLine(paragraph, 3);
		int[] columnWidths = { 15, 14, 14, 14, 14, 14, 14};
		PdfPTable table = new PdfPTable(noOfColumns);
		table.setTotalWidth(525);
		table.setWidths(columnWidths);
		table.setLockedWidth(true);
		for (int i = 0; i < noOfColumns; i++) {
			table.getDefaultCell().setPadding(1);
			Paragraph p = new Paragraph();
			p.setFont(HEADER_CONTENT_FONT);
			p.add(columnNames.get(i));
			table.addCell(p);
		}
		table.setHeaderRows(1);
		addTxnsData(table, ledgerDataBean);
		document.add(table);
	}

	private void addTxnsData(PdfPTable table, LedgerDataBean ledgerDataBean) {
		table.setWidthPercentage(100);
		table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
		table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.getDefaultCell().setPadding(1);
		table.addCell(new PdfPCell(new Paragraph(ledgerDataBean.getFromDate() == null ? "" : ledgerDataBean.getFromDate(), TABLE_CONTENT_FONT)));
		table.addCell(new PdfPCell(new Paragraph( "Opening Balance", TABLE_CONTENT_FONT)));
		table.addCell(new PdfPCell(new Paragraph( " " , TABLE_CONTENT_FONT)));
		table.addCell(new PdfPCell(new Paragraph( " " , TABLE_CONTENT_FONT)));
		table.addCell(new PdfPCell(new Paragraph( " " , TABLE_CONTENT_FONT)));
		table.addCell(new PdfPCell(new Paragraph( " " , TABLE_CONTENT_FONT)));
		table.addCell(new PdfPCell(new Paragraph( ledgerDataBean.getOpeningBalance() == null? "" : ledgerDataBean.getOpeningBalance().toString() , TABLE_CONTENT_FONT)));

		for (LedgerTxnBean ledgerTxnBean : ledgerDataBean.getTxns()) {
			table.setWidthPercentage(100);
			table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
			table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.getDefaultCell().setPadding(1);
			table.addCell(new PdfPCell(new Paragraph(ledgerTxnBean.getCreatedDate() == null ? "" : ledgerTxnBean.getCreatedDate() , TABLE_CONTENT_FONT)));
			table.addCell(new PdfPCell(new Paragraph(ledgerTxnBean.getTxnType() == null ? "" : ledgerTxnBean.getTxnType(), TABLE_CONTENT_FONT)));
			table.addCell(new PdfPCell(new Paragraph(ledgerTxnBean.getTxnDetail() == null ? "" : ledgerTxnBean.getTxnDetail(), TABLE_CONTENT_FONT)));
			table.addCell(new PdfPCell(new Paragraph(ledgerTxnBean.getOrderDate() == null ? "" : ledgerTxnBean.getOrderDate(), TABLE_CONTENT_FONT)));
			if(ledgerTxnBean.getTxnMode()!= null && ledgerTxnBean.getTxnMode().equals("DEBIT")){
				table.addCell(new PdfPCell(new Paragraph(ledgerTxnBean.getAmount() == null ? "" : ledgerTxnBean.getAmount().toString(), TABLE_CONTENT_FONT)));
				table.addCell(new PdfPCell(new Paragraph( "", TABLE_CONTENT_FONT)));
			}
			if(ledgerTxnBean.getTxnMode()!= null && ledgerTxnBean.getTxnMode().equals("CREDIT")){
				table.addCell(new PdfPCell(new Paragraph( "", TABLE_CONTENT_FONT)));
				table.addCell(new PdfPCell(new Paragraph(ledgerTxnBean.getAmount() == null ? "" : ledgerTxnBean.getAmount().toString(), TABLE_CONTENT_FONT)));
			}
			if (ledgerTxnBean.getTxnMode() == null) {
				table.addCell(new PdfPCell(new Paragraph("", TABLE_CONTENT_FONT)));
				table.addCell(new PdfPCell(new Paragraph( "", TABLE_CONTENT_FONT)));
			}
			table.addCell(new PdfPCell(new Paragraph(ledgerTxnBean.getBalance() == null? "" : ledgerTxnBean.getBalance().toString(), TABLE_CONTENT_FONT)));
		}
	}

	private void leaveEmptyLine(Paragraph paragraph, int number) {
		for (int i = 0; i < number; i++) {
			paragraph.add(new Paragraph(" "));
		}
	}

	private String getPdfName(String directory, String invoiceFileName) {
		return directory + "/" + invoiceFileName;
	}

	@Override
	public void onEndPage(PdfWriter writer, Document document) {
		PdfContentByte canvas = writer.getDirectContent();
		Rectangle rect = document.getPageSize();
		rect.setBorder(Rectangle.BOX);
		rect.setBorderWidth(5);
		rect.setBorderColor(BaseColor.RED);
		rect.setUseVariableBorders(true);
		canvas.rectangle(rect);
	}
}
