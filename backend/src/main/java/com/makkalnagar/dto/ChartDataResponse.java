package com.makkalnagar.dto;

import java.util.List;

public class ChartDataResponse {

    private SeriesData statusChart;
    private SeriesData categoryChart;
    private DepartmentWorkloadChart departmentWorkloadChart;

    public ChartDataResponse() {}

    public static class SeriesData {
        private List<String> labels;
        private List<Long> data;

        public SeriesData(List<String> labels, List<Long> data) {
            this.labels = labels;
            this.data = data;
        }

        public List<String> getLabels() { return labels; }
        public List<Long> getData() { return data; }
    }

    public static class DepartmentWorkloadChart {
        private List<String> labels;
        private List<Integer> capacity;
        private List<Long> activeCases;
        private List<Long> waitingCases;

        public DepartmentWorkloadChart(List<String> labels, List<Integer> capacity, List<Long> activeCases, List<Long> waitingCases) {
            this.labels = labels;
            this.capacity = capacity;
            this.activeCases = activeCases;
            this.waitingCases = waitingCases;
        }

        public List<String> getLabels() { return labels; }
        public List<Integer> getCapacity() { return capacity; }
        public List<Long> getActiveCases() { return activeCases; }
        public List<Long> getWaitingCases() { return waitingCases; }
    }

    public SeriesData getStatusChart() { return statusChart; }
    public void setStatusChart(SeriesData statusChart) { this.statusChart = statusChart; }

    public SeriesData getCategoryChart() { return categoryChart; }
    public void setCategoryChart(SeriesData categoryChart) { this.categoryChart = categoryChart; }

    public DepartmentWorkloadChart getDepartmentWorkloadChart() { return departmentWorkloadChart; }
    public void setDepartmentWorkloadChart(DepartmentWorkloadChart departmentWorkloadChart) { this.departmentWorkloadChart = departmentWorkloadChart; }
}
