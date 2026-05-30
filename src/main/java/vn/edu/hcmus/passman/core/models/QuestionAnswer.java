package vn.edu.hcmus.passman.core.models;

public class QuestionAnswer {
    private String question;
    private char[] answer;

    public QuestionAnswer(String question, char[] answer) {
        this.question = question;
        this.answer = answer;
    }

    public String getQuestion() {
        return question;
    }

    public char[] getAnswer() {
        return answer;
    }
}
