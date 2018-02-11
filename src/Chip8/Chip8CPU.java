package Chip8;

import com.sun.jdi.ShortType;
import org.omg.CORBA.portable.UnknownException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Random;

public class Chip8CPU {

    /* When we need to draw a character we turn on the relative pixels.
      0         1
      - 1111    - 0010
      - 1001    - 0110
      - 1001    - 0010
      - 1001    - 0010
      - 1111    - 0111
    */
    private short[] fontset =

            {
                    0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
                    0x20, 0x60, 0x20, 0x20, 0x70, // 1
                    0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
                    0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
                    0x90, 0x90, 0xF0, 0x10, 0x10, // 4
                    0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
                    0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
                    0xF0, 0x10, 0x20, 0x40, 0x40, // 7
                    0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
                    0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
                    0xF0, 0x90, 0xF0, 0x90, 0x90, // A
                    0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
                    0xF0, 0x80, 0x80, 0x80, 0xF0, // C
                    0xE0, 0x90, 0x90, 0x90, 0xE0, // D
                    0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
                    0xF0, 0x80, 0xF0, 0x80, 0x80, // F
            };

    private boolean drawFlag;
    private short[] gfx;

    private short[] V; // Registers[0-F].
    // NOTE: V[F] double as the:
    // As a flag for some instructions,
    // As a carry flag for others,
    // when drawing V[F] is set upon pixel collision
    private int X = 0x0F00;
    private int Y = 0x00F0;

    private short[] memory;
    private boolean[] key;

    private short soundTimer;
    private short delayTimer;

    private short[] stack;
    private byte stackPointer;

    private short opCode;
    private short indexR; // Register Index or just 'I'
    private short pc;     // Program Counter

    private Random random;

    public Chip8CPU() {
        Initialize();
    }

    public void Initialize() {
        pc = 0x200;
        indexR = 0;
        opCode = 0;
        stackPointer = 0;
        drawFlag = false;

        ClearMemory();
        ClearRegisters();
        ClearStack();
        ClearGraphics();
        ClearKey();
        ResetTimers();

        LoadFontSet();
    }

    public void Initialize(int seed) {
        pc = 0x200;
        indexR = 0;
        opCode = 0;
        stackPointer = 0;
        drawFlag = false;
        random = new Random();
        random.setSeed(seed);

        ClearMemory();
        ClearRegisters();
        ClearStack();
        ClearGraphics();
        ClearKey();
        ResetTimers();

        LoadFontSet();
    }

    public void LoadApplication(String FilePath) {
        File app = new File(FilePath);

        if (!app.exists())
            return;
        try {
            Initialize();

            FileInputStream inputStream = new FileInputStream(app);
            byte[] buffer = new byte[(int) app.length()];

            inputStream.read(buffer);

            for (int x = 0; x < buffer.length; x++)
                memory[x + 0x200] = buffer[x];

            inputStream.close();

        } catch (FileNotFoundException e) {

        } catch (Exception e) {
            System.out.print("Error ");
        }
    }

    public void EmulateCycle() {
        opCode = (short) (memory[pc] << 8 | memory[pc + 1]);

        ProcessOpCode(opCode);

        UpdateTimers();
    }
    // If the draw flag is true it tells us we need to update

    // what openGl needs to draw.
    public boolean getDrawFlag() {
        if (drawFlag) {
            drawFlag = false;
            return true;
        }

        return false;
    }

    public short[] getGraphicsInformation() {
        return gfx;
    }

    private void LoadFontSet() {
        for (int x = 0; x < fontset.length; x++)
            memory[x] = (byte) fontset[x];
    }

    private void DumpMemory() {

    }

    private void JumptoAddress(short address) {
        pc = address;
    }

    private void NextOperation() {
        pc += 2;
    }

    private void SkipOperation() {
        NextOperation();
        NextOperation();
    }

    private void UpdateTimers() {
        if (delayTimer > 0)
            delayTimer--;

        if (soundTimer > 0) {
            if (soundTimer == 1) {
                // https://stackoverflow.com/questions/29509010/how-to-play-a-short-beep-to-android-phones-loudspeaker-programmatically
                // ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                // toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);
            }
            soundTimer--;
        }
    }

    private int getRandom() {
        if (random == null)
            random = new Random();

        return random.nextInt() % 256;
    }

    private void ClearMemory() {

    }

    private void ClearRegisters() {

    }

    private void ClearStack() {

    }

    private void ClearGraphics() {

    }

    private void ClearKey() {

    }

    private void ResetTimers() {
    }

    /*
    OpCode Notes: The Chip-8 has 35 opcodes. Two bytes longs and stored big-endian.
    NNN: address
    NN: 8-bit constant
    N: 4-bit constant
    X and Y are 4-bit register identifers
    PC: programCounter.
    (indexR) I: indexer for 16bit registers
     */

    private void ProcessOpCode(short Code) {
        switch (Code & 0xF000) {
            case 0x0000:

                OpCode_0x0000(Code);
                return;
            case 0x1000:
                /// Jumps to address NNN
                JumptoAddress((short) (opCode & 0x0FFF));
                return;
            case 0x2000:
                /// Calls subroutine at NNN
                stack[stackPointer] = pc;
                stackPointer++;
                JumptoAddress((short) (opCode & 0x0FFF));
                return;

            case 0x3000:
                /// Skips the next instruction if VX equals NN.
                if (V[(Code & 0x0F00) >> 8] == (opCode & 0x00FF))
                    SkipOperation();
                else
                    NextOperation();
                return;

            case 0x4000:
                // Skips the next instruction if VX does not equal NN
                if (V[(Code & 0x0F00) >> 8] != (Code >> 0xFF))
                    SkipOperation();
                else
                    NextOperation();
                return;

            case 0x5000:
                // Skips the next instruction if VX equals VY
                if (V[(Code & 0x0F00) >> 8] == V[(Code & 0x0F00 >> 4)])
                    SkipOperation();
                else
                    NextOperation();
                return;

            case 0x6000:
                // Sets VX to NN
                V[(Code & 0x0F00) >> 8] = (byte) (Code & 0x00FF);
                NextOperation();
                return;

            case 0x7000:
                // Adds NN to VX (Carry flag is not changed)
                V[(Code & 0x0F00) >> 8] += (byte) (Code & 0x00FF);
                NextOperation();
                return;

            case 0x8000:
                OpCode_0x8000(Code);
                return;

            case 0x9000:
                // Skips the next instruction if VX does not equal VY
                if (((Code & 0x0F00) >> 8) != ((Code & 0x00F0) >> 4))
                    SkipOperation();
                else
                    NextOperation();
                return;

            case 0xA000:
                // Sets I to the address NNN
                indexR = (short) (Code & 0x0FFF);
                NextOperation();
                return;

            case 0xB000:
                // Jumps to the address NNN plus V[0]
                pc = (short) (Code & 0x0FFF + V[0]);
                return;

            case 0xC000:
                // Sets VX to the result of a bitwise 'and' operation on a random number and NN.
                V[(Code & 0x0F00) >> 8] = (byte) (getRandom() & (Code & 0x00FF));
                NextOperation();
                return;

            case 0xD000:
                // Draws a sprite at coordinate (VX, VY) that has a width of 8 pixels and a height of N pixels.
                // Each row of 8 pixels is read as bit-coded starting from memory location I; I value doesn’t
                // change after the execution of this instruction. As described above, VF is set to 1 if any screen
                // pixels are flipped from set to unset when the sprite is drawn, and to 0 if that doesn’t happen
                Draw(Code);
                return;

            case 0xE000:
                Input(Code);
                return;

            case 0xF000:
                OpCode_0xF000(Code);
                return;
        }
    }

    private void Draw(short Code) {
        int X = (Code & 0x0F00 >> 8);
        int Y = (Code & 0x00F0 >> 4);
        int Width = 8;
        int Height = (Code & 0x000F);
        short pixel;

        V[0xF] = 0;
        for (int yLine = 0; yLine < Height; yLine++) {
            pixel = memory[indexR + yLine];

            for (int xLine = 0; xLine < Width; xLine++) {
                if ((pixel & (0x80 >> xLine)) != 0) {
                    if (gfx[X + xLine + ((Y + yLine) * 64)] == 1)
                        V[0xF] = 1;
                    gfx[X + xLine + Y + yLine * 64] ^= 1;
                }
            }
        }

        drawFlag = true;
        NextOperation();
    }

    private void Input(short Code) {
        switch (opCode & 0x000F) {
            case 0x000E:
                // Skips the next instuction if the key stored in VX is pressed
                // Note: Typicaly the next instruction is going to be a skip in operations.
                if (V[(Code & 0x0F00) >> 8] != 0)
                    SkipOperation();
                else
                    NextOperation();
                return;

            case 0x0001:
                // Skips the next instuction if the key stored in VX is not pressed
                // Note: Typicaly the next instruction is going to be a skip in operations.
                if (V[(Code & 0x0F00) >> 8] == 0)
                    SkipOperation();
                else
                    NextOperation();
                return;
        }
    }

    private void OpCode_0x0000(short Code) {
        switch (opCode & 0x000F) {
            case 0x0000:
                // Clears the screen
                ClearGraphics();
                NextOperation();
                return;

            case 0x000E:
                // Returns from a subroutine
                stackPointer--;
                JumptoAddress(stack[stackPointer]);
                return;

            default:
                System.out.print("Unknown opcode: " + opCode + ". Can not be parsed.");
                try {
                    Thread.sleep(10000);
                    System.exit(-1);
                } catch (InterruptedException e) {
                }
        }
    }

    private void OpCode_0x8000(short Code) {
        switch (Code & 0x000F) {
            case 0x0000:
                // Sets VX to the value of VY
                V[(Code & 0x0F00) >> 8] = V[(Code & 0x00F0) >> 4];
                NextOperation();
                return;

            case 0x0001:
                // Sets VX to the value of VY. (Bitwise OR operation)
                V[(Code & 0x0F00) >> 8] |= V[(Code & 0x00F0) >> 4];
                NextOperation();
                return;

            case 0x0002:
                // Sets VX to VX and VY. (Bitwise AND operation)
                V[(Code & 0x0F00) >> 8] &= V[(Code & 0x00F0) >> 4];
                NextOperation();
                return;

            case 0x0003:
                // Sets VX to VX xor VY.
                V[(Code & 0x0F00) >> 8] ^= V[(Code & 0x00F0) >> 4];
                NextOperation();
                return;

            case 0x0004:
                // Adds VY to VX. VF is set to 1 when there's a carry, and to 0 when there isn't.
                // Note: We do this by checking if V[X] is less than V[Y].
                // Flow:  * First: Start off by adding V[Y] to V[X].
                //        * Second: Since java does not support an usigned byte type we have to scrub any value beyond
                //              beyond 255( 0x00FF ).
                //        * Third: We than check to see if V[Y] is greater than V[X]. If V[Y] is greater than V[x] it
                //              means we overflowed above of 255.
                //        * Forth: We set V[F]'s bit indicator. 1 if overflow is detected, and 0 if an overflow did not
                //              happen.

                /*
                    byte X = 250;
                    byte Y = 15;

                    X = X + Y;
                    X = 9;
                 */
                V[(Code & 0x0F00) >> 8] += V[(Code & 0x00F0) >> 4];
                V[(Code & X) >> 8] &= 0x00FF;
                if (V[(Code & 0x0FF) >> 8] < V[(Code & 0x00F0) >> 4])
                    V[0xF] = 1;
                else
                    V[0xF] = 0;
                NextOperation();
                return;

            case 0x0005:
                // VY is subtracted from VX. VF is set to 0 when there's a borrow, and 1 when there isn't.
                // Note: We do this by checking if V[X] is greater than V[Y].
                // Flow:  * First: Start off by adding V[Y] to V[X].
                //        * Second: Since java does not support an usigned byte type we have to scrub any value beyond
                //              beyond 255( 0x00FF ).
                //        * Third: We than check to see if V[Y] is greater than V[X]. If V[Y] is greater than V[x] it
                //              means we underflowed and the value dipped below 0.
                //        * Forth: We set V[F]'s bit indicator. 0 if underflow is detected, and 1 if an overflow did not
                //              happen.
                /*
                    X = 15;
                    Y = 250;

                    X = X - Y;
                    X = 31
                 */
                V[(Code & 0x0F00) >> 8] -= V[(Code & 0x00F0) >> 4];
                V[(Code & 0x0F00) >> 8] &= 0x00FF;
                if (V[(Code & 0x0F00) >> 8] > V[(Code & 0x00F0) >> 4])
                    V[0xF] = 0;
                else
                    V[0xF] = 1;
                NextOperation();
                return;

            case 0x0006:
                // Shifts VY right by one and copies the result to VX. VF is set to the value of the least significant
                // bit of VY before the shift.[2]
                V[0x000F] = (short) ((V[(Code & Y) >> 4]) & 0x0001);
                V[(Code & X) >> 8] = (short) (V[(Code & Y) >> 4] >> 1);
                V[(Code & X) >> 8] = 0x00FF;
                NextOperation();
                return;

            case 0x0007:
                // Opcode: 8XY7
                // Sets VX to VY minus VX. VF is set to 0 when there's a borrow, and 1 when there isn't.
                // Notes:
                //      First: we create a tmp value with the value of V[Y] - V[X].
                //      Second: We than test the value against V[X].
                //      Third: We than test the tmp value against V[X], if V[X] is less than the tmp value we set the
                //              flag on V[0xF] = 1, or to 0. Math proof is below.
                //
                // Same concept as:
                //      case 0x0004 and case 0x0005
                //
                // / x = 4
                // y = 5
                // tmp = y - x
                // <value of tmp is 255 (ubyte)>
                // if(tmp < x)
                //  V[0xF]  = 0;
                // else
                //  V[0xF] = 1;
                //
                short Value = (short) ((V[(Code & X) >> 8] - V[(Code & X) >> 8]) & 0x00FF);

                if (Value < V[(Code & X) >> 8])
                    V[0xF] = 0;
                else
                    V[0xF] = 1;

                V[(Code & X) >> 8] = (short) (V[(Code & Y) >> 4] - V[(Code & X) >> 8]);
                V[(Code & X) >> 8] &= 0x00FF;
                NextOperation();
                return;

            case 0x000E:
                // Shifts VY left by one and copies the result to VX. VF is set to the value of the most significant
                // bit of VY before the shift.
                V[0xF] = (short) (V[(Code & Y) >> 4] & 0x80);
                V[(Code & X) >> 8] = (short) ((V[(Code & Y) >> 4] << 1) * 0xFF);
                NextOperation();
                return;

            default:
                System.out.print("Unknown opcode: " + opCode + ". Can not be parsed.");
                try {
                    Thread.sleep(10000);
                    System.exit(-1);
                } catch (InterruptedException e) {
                }
        }
    }

    private void OpCode_0xF000(short Code) {
        switch (Code & 0x000F) {
            case 0x0007:
                // Sets VX to the value of the delay timer.
                V[(Code & X) >> 8] = (short) delayTimer;
                NextOperation();
                return;

            case 0x000A:
                // A key press is awaited, and then stored in VX. (Blocking Operation.
                // All instruction halted until next key event)
                for (int x = 0; x < key.length; x++) {
                    if (key[x]) {
                        V[(Code & 0x0F00) >> 8] = (short) x;
                        NextOperation();
                        return;
                    }
                }
                return;

            case 0x0015:
                delayTimer = V[(Code & X) >> 8];
                NextOperation();
                return;

            case 0x0018:
                soundTimer = V[(Code & X) >> 8];
                NextOperation();
                return;

            case 0x001E:
                indexR += V[(Code & X) >> 8];
                NextOperation();
                return;

            case 0x0029:
                indexR = (short)(V[(Code & X) >> 8] * 0x5);
                NextOperation();
                return;

            case 0x0033:
                // Stores the binary-coded decimal representation of VX, with the most significant of three digits at
                // the address in I,  the middle digit at I plus 1, and the least significant digit at I plus 2.
                // (In other words, take the decimal representation of VX, place the hundreds digit in memory at
                // location in I, the tens digit at location I+1, and the ones digit at location I+2.)
                NextOperation();
                return;

            case 0x0055: {
                int endingIndex = V[(Code & X) >> 8];
                for (int x = 0; x <= endingIndex; x++) {
                    memory[indexR++] = (short)V[x];
                }
                NextOperation();
                return;
            }
            case 0x0065: {
                int endingIndex = V[(Code & X) >> 8];
                for (int x = 0; x <= endingIndex; x++) {
                    V[x] = memory[indexR++];
                }
                NextOperation();
                return;
            }
            default:
                System.out.print("Unknown opcode: " + opCode + ". Can not be parsed.");
                try {
                    Thread.sleep(10000);
                    System.exit(-1);
                } catch (InterruptedException e) {
                }
        }
    }
}