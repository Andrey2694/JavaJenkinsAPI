public class Main {
    public static void main(String[] args) throws Exception {
        JenkinsJobManager jenkinsJobManager = new JenkinsJobManager();
        jenkinsJobManager.changeChoiceParameter("TestAPi", "Param1", "666666666");
//        jenkinsJobManager.changeGitData("TestAPi", "https://github.com/21212121", "newCreds1212121", "newBranch212121");
    }
}
