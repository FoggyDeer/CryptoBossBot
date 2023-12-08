package cryptobossbot;


import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Console {
    public Set<String> commands = new HashSet<>(List.of(
            "@sendMessageTo",
            "@notifyAll",
            "@printAllUsers",
            "@printAuthorizedUsers",
            "@printLoggingUsers",
            "@printBannedUsers",
            "@banUser",
            "@unbanUser",
            "@resetUser",
            "@last",
            "@?"));
    private final Bot bot;

    private Command lastCommand;

    private void printLastCommand(){
        if(lastCommand == null)
            System.out.println(" ");
        else
            System.out.println(lastCommand);
    }

    Console(Bot bot){
        this.bot = bot;
        Thread consoleThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            String str;
            Command command;
            while (true) {
                str = scanner.nextLine();
                try {
                    command = Command.parseCommand(str);
                    execute(command);
                    lastCommand = command;
                } catch (NoSuchCommandFoundException | Command.NoCommandEnteredException e){
                    Console.error(e.getMessage());
                }
            }
        });
        consoleThread.start();
    }

    public String makeSentence(List<String> args){
        StringBuilder sb = new StringBuilder();
        for(String str : args){
            if(sb.length() > 0)
                sb.append(" ");
            sb.append(str);
        }
        return sb.toString();
    }
    public void execute(Command command){
        if(!commands.contains(command.getCommandName()))
            throw new NoSuchCommandFoundException();
        else {
            switch (command.getCommandName()) {
                case "@sendMessageTo" -> {
                    if(command.getArguments().size() < 2)
                        error("Wrong number of arguments.");
                    else {
                        try {
                            bot.sendMessage(Long.parseLong(command.getArguments().get(0)), makeSentence(command.getArguments().subList(1, command.getArguments().size())));
                        } catch (NumberFormatException e) {
                            Console.error("Wrong id format provided.");
                        }
                    }
                }
                case "@notifyAll" -> {
                    if(command.getArguments().size() < 1)
                        error("Wrong number of arguments.");
                    else
                        bot.notifyAll(makeSentence(command.getArguments()));
                }
                case "@printAllUsers" -> {
                    bot.printAllUsers();
                }
                case "@printAuthorizedUsers" -> {
                    bot.printAllAuthorizedUsers();
                }
                case "@printLoggingUsers" -> {
                    bot.printAllLoggingUsers();
                }
                case "@printBannedUsers" -> {
                    bot.printAllBannedUsers();
                }
                case "@banUser" -> {
                    try {
                        if(command.getArguments().size() < 1)
                            error("Wrong number of arguments.");
                        else
                            bot.banUser(command.getArguments().get(0));
                    } catch (NumberFormatException e) {
                        Console.error("Wrong id format provided.");
                    }
                }case "@unbanUser" -> {
                    try {
                        if(command.getArguments().size() < 1)
                            error("Wrong number of arguments.");
                        else
                            bot.unbanUser(command.getArguments().get(0));
                    } catch (NumberFormatException e) {
                        Console.error("Wrong id format provided.");
                    }
                }
                case "@resetUser" -> {
                    try {
                        if(command.getArguments().size() < 1)
                            error("Wrong number of arguments.");
                        else {
                            bot.resetUser(command.getArguments().get(0));
                            bot.saveUser(bot.getUsers().get(Long.parseLong(command.getArguments().get(0))));
                        }
                    } catch (NumberFormatException e) {
                        Console.error("Wrong id format provided.");
                    } catch (IOException ignored) {}
                }
                case "@last" -> {
                    printLastCommand();
                }
                case "@?" -> {
                    for(String str : commands){
                        System.out.println(str);
                    }
                }
            }
        }
    }

    private static class Command {
        private String commandName;

        private final ArrayList<String> arguments = new ArrayList<>();

        public static Command parseCommand(String text){
            Command command = new Command();
            Pattern pattern_1 = Pattern.compile("^\\s*(@\\S+)(.*)");
            Pattern pattern_2 = Pattern.compile("(\\S+)");
            Matcher matcher = pattern_1.matcher(text);

            if(matcher.find()){
                command.setCommandName(matcher.group(1));
                matcher = pattern_2.matcher(matcher.group(2).trim());

                while (matcher.find()){
                    if(matcher.group(1) != null)
                        command.addArgument(matcher.group(1));
                }
            } else {
                throw new NoCommandEnteredException();
            }
            return command;
        }

        public static class NoCommandEnteredException extends RuntimeException{
            NoCommandEnteredException(){
                super("Command was not entered.");
            }
        }

        public void setCommandName(String command) {
            this.commandName = command;
        }

        public void addArgument(String arg) {
            this.arguments.add(arg);
        }

        public String getCommandName() {
            return commandName;
        }

        public ArrayList<String> getArguments() {
            return arguments;
        }

        @Override
        public String toString() {
            return commandName + " " + arguments;
        }
    }
    public static class NoSuchCommandFoundException extends RuntimeException{
        NoSuchCommandFoundException(){
            super("No matching command found.");
        }
    }

    public static void success(String str){
        System.out.println("\u001B[32m"+str+"\u001B[0m");
    }

    public static void error(String str){
        System.out.println("\u001B[31m"+str+"\u001B[0m");
    }
}
