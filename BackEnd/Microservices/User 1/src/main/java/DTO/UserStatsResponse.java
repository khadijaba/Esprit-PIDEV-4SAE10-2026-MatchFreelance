package DTO;

public class UserStatsResponse {
    
    private long totalUsers;
    private long activeFreelancers;
    private long activeProjectOwners;
    private long activeAccounts;
    private long inactiveAccounts;
    private long totalAdmins;

    public UserStatsResponse() {}

    public UserStatsResponse(long totalUsers, long activeFreelancers, long activeProjectOwners, 
                              long activeAccounts, long inactiveAccounts, long totalAdmins) {
        this.totalUsers = totalUsers;
        this.activeFreelancers = activeFreelancers;
        this.activeProjectOwners = activeProjectOwners;
        this.activeAccounts = activeAccounts;
        this.inactiveAccounts = inactiveAccounts;
        this.totalAdmins = totalAdmins;
    }

    // Getters and setters
    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
    
    public long getActiveFreelancers() { return activeFreelancers; }
    public void setActiveFreelancers(long activeFreelancers) { this.activeFreelancers = activeFreelancers; }
    
    public long getActiveProjectOwners() { return activeProjectOwners; }
    public void setActiveProjectOwners(long activeProjectOwners) { this.activeProjectOwners = activeProjectOwners; }
    
    public long getActiveAccounts() { return activeAccounts; }
    public void setActiveAccounts(long activeAccounts) { this.activeAccounts = activeAccounts; }
    
    public long getInactiveAccounts() { return inactiveAccounts; }
    public void setInactiveAccounts(long inactiveAccounts) { this.inactiveAccounts = inactiveAccounts; }
    
    public long getTotalAdmins() { return totalAdmins; }
    public void setTotalAdmins(long totalAdmins) { this.totalAdmins = totalAdmins; }
}
