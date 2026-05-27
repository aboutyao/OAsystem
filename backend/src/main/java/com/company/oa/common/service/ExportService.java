package com.company.oa.common.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.WriteSheet;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class ExportService {

    public void exportExcel(HttpServletResponse response, String fileName, String sheetName,
                            List<String> headers, List<List<Object>> data) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");

            EasyExcel.write(response.getOutputStream())
                    .head(buildHead(headers))
                    .sheet(sheetName)
                    .doWrite(data);
        } catch (Exception e) {
            throw new RuntimeException("Export failed: " + e.getMessage(), e);
        }
    }

    public void exportMapExcel(HttpServletResponse response, String fileName, String sheetName,
                               List<String> headers, List<? extends Map<String, Object>> dataList,
                               List<String> keys) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");

            EasyExcel.write(response.getOutputStream())
                    .head(buildHead(headers))
                    .sheet(sheetName)
                    .doWrite(toRows(dataList, keys));
        } catch (Exception e) {
            throw new RuntimeException("Export failed: " + e.getMessage(), e);
        }
    }

    private List<List<String>> buildHead(List<String> headers) {
        return headers.stream().map(h -> List.of(h)).toList();
    }

    private List<List<Object>> toRows(List<? extends Map<String, Object>> dataList, List<String> keys) {
        return dataList.stream().map(row ->
            keys.stream().map(k -> row.getOrDefault(k, "")).toList()
        ).toList();
    }
}
