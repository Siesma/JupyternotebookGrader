package helper;

import java.io.IOException;

public class TestApp {

    public static void main(String[] args) throws IOException {
        String handin = "/home/tom/Documents/Uni/GDP/HS25/exercise_6b.ipynb";
        String solution = "/home/tom/Documents/Uni/GDP/HS25/gdp_25_final_solution.ipynb";

        Notebook handinNotebook = new Notebook(handin, NotebookType.HANDIN);
        Notebook solutionNotebook = new Notebook(solution, NotebookType.SOLUTION);

        System.out.println("=========== SOLUTION NOTEBOOK ===========");
        for(NotebookCell cell : solutionNotebook.getCells()){
            System.out.println(cell.determineGradingSchemeCellIndex());
        }
        solutionNotebook.getCells().get(6).printRoleMappingDetected();

//        solutionNotebook.getCells().get(6).getCells().forEach(System.out::println);

//        System.out.println("\n=========== HANDIN NOTEBOOK ===========");
//        handinNotebook.getCells().getFirst().printRoleMappingDetected();

    }
}
