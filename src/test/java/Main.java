public class Main {
    public static void main(String[] args) throws Exception {
        JenkinsJobManager jenkinsJobManager = new JenkinsJobManager();
//        jenkinsJobManager.changeChoiceParameter("TestAPi", "Param1", "143242424234234");
        jenkinsJobManager.changeGitData("TestAPi", "https://github.com/21212121", "newCreds1212121", "newBranch212121");
    }
}
