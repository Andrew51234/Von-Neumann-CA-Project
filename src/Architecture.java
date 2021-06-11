public class Architecture {
    private static int [] mainMem = new int [2048]; // Data_location = Location + 1024
    private static int [] registers  = new int[33];
    private static int clk;
    private static int numOfIns;
    private static boolean hasIF;
    private static boolean hasID;
    private static boolean hasEXR;
    private static boolean hasEXI;
    private static boolean hasEXJ;
    private static boolean hasMEMR;
    private static boolean hasMEMW;
    private static boolean hasFakeMEM;
    private static boolean hasWB;
    private static boolean hasFakeWB;
    private static int nextDecode;
    private static int nextOpcode;
    private static int nextR1;
    private static int nextR2;
    private static int nextR3;
    private static int nextShamt;
    private static int nextImmediate;
    private static int nextAddress;
    private static String fakeMemReg;
    private static int fakeMemValue;
    private static String wRegReg;
    private static int wRegValue;
    private static int realWord;
    private static int realValue;
    private static String completeRunReg;



    public Architecture(){

        registers[0] = 0;  //Zero register
        registers[32] = 0; //PC register
        clk = 1;
        hasIF = true;
        hasID = false;
        hasEXR = false;
        hasEXI = false;
        hasEXJ = false;
        hasMEMR = false;
        hasMEMW = false;
        hasFakeMEM = false;
        hasWB = false;
        //add all the instructions to their appropriate locations in the memory and adjust the numOfIns value
        numOfIns = 1;
    }

    public static void writeRegister(String register, int value) throws ArchitectureExceptions {
        if(register.equals("R0")) {
            System.out.println("The zero register cannot be overwritten");
            return;
        }

        if(register.equals("PC")){
            registers[32] = value;
        }
        else if(register.charAt(0)=='R' && Integer.parseInt(register.substring(1))>0 && Integer.parseInt(register.substring(1))<32){
            registers[Integer.parseInt(register.substring(1))] = value;
        }
    }

    public static int readRegister(String register) throws ArchitectureExceptions {
        if(register.equals("PC")){
            return registers[32];
        }
        else if(register.charAt(0)=='R' && Integer.parseInt(register.substring(1))>=0 && Integer.parseInt(register.substring(1))<32){
            return registers[Integer.parseInt(register.substring(1))];
        }
        return 0;
    }

    public static void writeMem(int word, int value, boolean instruction) throws ArchitectureExceptions {

        if (instruction){
            if (word<=1023 && word>=0)
                mainMem[word] = value;
            else{
                System.out.println("Invalid instruction location (Accessed data memory)");
                return;
            }
        }

        else {
            if (word >= 1024 || word<0){
                System.out.println("Out of memory bounds");
                return;
            }
            else
                mainMem[word+1024] = value;
        }
    }

    public static int readMem(int word, boolean instruction) throws ArchitectureExceptions {
        if (instruction){
            if (word<=1023 && word>=0)
                return mainMem[word];
            else {
                System.out.println("Invalid instruction location (Accessed data memory)");
                return readRegister("PC");
            }
        }

        else {
            if (word >= 1024 || word<0) {
                System.out.println("Out of memory bounds");
                return readRegister("PC");
            }
            else
                return mainMem[word+1024];
        }
    }

    public static void fakeMemAccess(String register, int value) throws ArchitectureExceptions {
        writeRegister(register, value);
        wRegReg = register;
        wRegValue = value;
        hasWB = true;
    }

    public static void realMemWrite(int word, int value, boolean instruction) throws ArchitectureExceptions {
        writeMem(word, value, instruction);
        hasFakeWB = true;

    }

    public static void realMemRead(String reg, int word, boolean instruction) throws ArchitectureExceptions {
        wRegValue = readMem(word, instruction);
        wRegReg = reg;
        hasWB = true;
    }

    public static  void fakeWB(){
        return;
    }

    public static void fetch() throws ArchitectureExceptions {
        int oldClk = clk;
        int pc = readRegister("PC");
        int instruction = 0;

        if(pc<1023) {
            instruction = readMem(pc, true);
        }
        else {
            writeRegister("PC",0);
        }

        System.out.println("fetched: "+instruction);

        writeRegister("PC", pc+1);
        nextDecode = instruction;
        hasID = true;
    }

    public static void decode(int instruction) throws ArchitectureExceptions {

        int opcode = 0;      // bits31:28
        int r1 = 0;          // bits27:23
        int r2 = 0;          // bits22:18
        int r3 = 0;          // bits17:13
        int shamt = 0;       // bits12:0
        int immediate = 0;   // bits17:0
        int address = 0;     // bits27:0

        int valueR1 = 0;
        int valueR2 = 0;
        int valueR3 = 0;

        //bit-masking

        opcode    = (instruction & 0b11110000000000000000000000000000) >> 28;
        r1        = (instruction & 0b00001111100000000000000000000000) >> 23;
        r2        = (instruction & 0b00000000011111000000000000000000) >> 18;
        r3        = (instruction & 0b00000000000000111110000000000000) >> 13;
        shamt     = (instruction & 0b00000000000000000001111111111111);
        immediate = (instruction & 0b00000000000000111111111111111111);
        address   = (instruction & 0b00001111111111111111111111111111);

        System.out.println("Decoded: "+instruction);
        if (opcode == 0 || opcode == 1 || opcode == 8 || opcode == 9){
            //execR(opcode, r1, r2, r3, shamt);
            nextOpcode = opcode;
            nextR1= r1;
            nextR2= r2;
            nextR3= r3;
            nextShamt= shamt;
            hasEXR = true;
            hasEXI = false;
            hasEXJ = false;

        }

        else if (opcode == 2 || opcode == 3 || opcode == 4 || opcode == 5 || opcode == 6 || opcode == 10 || opcode == 11){
            nextOpcode = opcode;
            nextR1= r1;
            nextR2= r2;
            nextImmediate= immediate;
            hasEXR = false;
            hasEXI = true;
            hasEXJ = false;
        }
        else if (opcode == 7){
            nextAddress = address;
            hasEXR = false;
            hasEXI = false;
            hasEXJ = true;
        }
    }

    public static void execR(int opcode, int r1, int r2, int r3, int shamt) throws ArchitectureExceptions {

        String r1Pos = "R"+r1;
        int r2Value = readRegister("R"+r2);
        int r3Value = readRegister("R"+r3);

        if (opcode == 0){  //ADD
            fakeMemReg = r1Pos;
            fakeMemValue = r2Value + r3Value;
            hasFakeMEM = true;
        }

        if (opcode == 1){  //SUB
            fakeMemReg = r1Pos;
            fakeMemValue = r2Value - r3Value;
            hasFakeMEM = true;
        }

        if (opcode == 8){  //SLL
            fakeMemReg = r1Pos;
            fakeMemValue = r2Value << shamt;
            hasFakeMEM = true;
        }

        if (opcode == 9){  //SRL
            fakeMemReg = r1Pos;
            fakeMemValue = r2Value >> shamt;
            hasFakeMEM = true;
        }
    }

    public static void execI(int opcode, int r1, int r2, int immediate) throws ArchitectureExceptions {

        String r1Pos = "R"+r1;
        int r1Value = readRegister("R"+r1);
        int r2Value = readRegister("R"+r2);

        if (opcode == 2){  //MULI
            fakeMemReg = r1Pos;
            fakeMemValue = r2Value * immediate;
            hasFakeMEM = true;
        }

        if (opcode == 3){  //ADDI
            fakeMemReg = r1Pos;
            fakeMemValue = r2Value + immediate;
            hasFakeMEM = true;
        }

        if (opcode == 4){  //BNE
            if(r1Value != r2Value){
                fakeMemReg = "PC";
                fakeMemValue = readRegister("PC") + 1 + immediate;
                hasFakeMEM = true;
            }
        }

        if (opcode == 5){  //ANDI
            fakeMemReg = r1Pos;
            fakeMemValue = r2Value & immediate;
            hasFakeMEM = true;
        }

        if (opcode == 6){  //ORI
            fakeMemReg = r1Pos;
            fakeMemValue = r2Value | immediate;
            hasFakeMEM = true;
        }

        if (opcode == 10){  //LW
            writeRegister(r1Pos, readMem(r2Value + immediate, false));
            completeRunReg = r1Pos;
            realWord = r2Value + immediate;
            hasMEMR = true;
        }

        if (opcode == 11){  //SW
            realWord = r2Value + immediate;
            realValue = r1Value;
            hasMEMW = true;
        }

    }

    public static void execJ(int address) throws ArchitectureExceptions {  //J

        int newPC  = (readRegister("PC") & 0b11110000000000000000000000000000) >> 28;
        String newPCStr = Integer.toBinaryString(newPC);
        String addressStr = Integer.toBinaryString(address);
        int value = Integer.parseInt((newPCStr + addressStr),2);

        fakeMemReg = "PC";
        fakeMemValue = value;
        hasFakeMEM = true;

        System.out.println("jumped to "+readRegister("PC"));
    }

    public static void dispatcher() throws ArchitectureExceptions {
        int totalClks = 7 + ((numOfIns-1)*2);
        while(clk<=totalClks){
            if(!hasIF && !hasID && !hasEXR && !hasEXI && !hasEXJ && !hasMEMR && !hasMEMW && !hasFakeMEM && !hasWB){
                return;
            }
            if(clk>=(totalClks-5)){
                hasIF = false;
                if(clk>=(totalClks-3)){
                    hasID = false;
                    if(clk>=(totalClks-1)){
                        hasEXR = false;
                        hasEXI = false;
                        hasEXJ = false;
                        if(clk==totalClks){
                            hasMEMR = false;
                            hasMEMW = false;
                            hasFakeMEM = false;
                        }
                    }
                }
            }
            if(hasFakeWB){
                fakeWB();
            }
            if(hasWB){
                writeRegister(wRegReg, wRegValue);
            }
            if(hasFakeMEM){
                fakeMemAccess(fakeMemReg, fakeMemValue);
            }
            if(hasMEMW){
                realMemWrite(realWord, realValue, false);
            }
            if(hasMEMR){
                realMemRead(completeRunReg, realWord, false);
            }
            if(hasEXR){
                execR(nextOpcode, nextR1, nextR2, nextR3, nextShamt);
            }
            if(hasEXI){
                execI(nextOpcode, nextR1, nextR2, nextImmediate);
            }
            if(hasEXJ){
                execJ(nextAddress);
            }
            if(hasID){
                decode(nextDecode);
            }
            if(hasIF){
                fetch();
            }
            clk++;
            if(clk%2==1){
                hasIF = true;
            }
            else{
                hasIF = false;
            }
        }
    }
}
