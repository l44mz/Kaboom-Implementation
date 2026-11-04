/*
 * ILI9486 on a 16-bit parallel (8080) bus, driven straight from the Raspberry Pi 4's
 * GPIO registers through /dev/gpiomem. Python calls it with ctypes (see ili9486.py).
 *
 * Build on the Pi (from the robot folder):
 *   gcc -O2 -shared -fPIC -o kaboom_robot/display/libili9486.so kaboom_robot/display/ili9486_par.c
 *
 * Why C: one screen is 480 x 320 = 153,600 pixels. Each pixel needs a few register
 * writes, which takes about 20-40 ms here but several seconds from pure Python.
 */
#include <fcntl.h>
#include <stdint.h>
#include <sys/mman.h>
#include <time.h>
#include <unistd.h>

/* BCM2711 GPIO register offsets, in 32-bit words. */
#define GPFSEL0 0  /* function select, 10 pins per register, 3 bits each */
#define GPSET0 7   /* write 1 = drive the pin high */
#define GPCLR0 10  /* write 1 = drive the pin low */
#define GPLEV0 13  /* read the pin levels (used as a tiny delay) */

static volatile uint32_t *gpio = 0;

/* For every 16-bit value: which pins go high and which go low. 512 KB, built once. */
static uint32_t set_lut[65536];
static uint32_t clr_lut[65536];

static uint32_t rs_mask, wr_mask, cs_mask, rst_mask, rd_mask;
static int delay_reads = 1;
static int data_pin[16];

/* 0 = standard ILI9486 (RS low = command). 1 = RS high = command (if your module is wired that way). */
static int rs_invert = 0;

static void sleep_ms(int ms) {
    struct timespec t = {ms / 1000, (long)(ms % 1000) * 1000000L};
    nanosleep(&t, 0);
}

static void make_output(int pin) {
    int reg = GPFSEL0 + pin / 10;
    int shift = (pin % 10) * 3;
    gpio[reg] = (gpio[reg] & ~(7u << shift)) | (1u << shift);
}

static void make_input(int pin) {
    int reg = GPFSEL0 + pin / 10;
    int shift = (pin % 10) * 3;
    gpio[reg] = gpio[reg] & ~(7u << shift);
}

/* RS level for a command, and for data/parameters. */
static inline void rs_command(void) {
    if (rs_invert) gpio[GPSET0] = rs_mask;
    else gpio[GPCLR0] = rs_mask;
}

static inline void rs_data(void) {
    if (rs_invert) gpio[GPCLR0] = rs_mask;
    else gpio[GPSET0] = rs_mask;
}

/* A few register reads: slows the bus down if the panel can't keep up. */
static inline void bus_pause(void) {
    for (int i = 0; i < delay_reads; i++) {
        (void)gpio[GPLEV0];
    }
}

/* One write cycle: put the value on D0-D15 while WR is low, then raise WR (the panel latches). */
static inline void bus_write(uint16_t value) {
    gpio[GPCLR0] = clr_lut[value] | wr_mask;
    gpio[GPSET0] = set_lut[value];
    bus_pause();
    gpio[GPSET0] = wr_mask;
    bus_pause();
}

/*
 * Opens /dev/gpiomem and sets the pins up.
 * data_pins: 16 BCM GPIO numbers for D0..D15. rd: -1 if RD is wired to 3.3V instead.
 * delay: extra register reads per write (1 is safe; 0 is faster; raise it if pixels look wrong).
 * Returns 0 on success, negative on error.
 */
int ili_open(const int *data_pins, int rs, int wr, int cs, int rst, int rd, int delay) {
    int fd = open("/dev/gpiomem", O_RDWR | O_SYNC);
    if (fd < 0) return -1;
    void *map = mmap(0, 4096, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    close(fd);
    if (map == MAP_FAILED) return -2;
    gpio = (volatile uint32_t *)map;

    int control[5] = {rs, wr, cs, rst, rd};
    for (int i = 0; i < 16; i++) {
        if (data_pins[i] < 0 || data_pins[i] > 27) return -3;
    }
    for (int i = 0; i < 5; i++) {
        if (control[i] > 27 || (control[i] < 0 && i != 4)) return -3;
    }

    delay_reads = delay < 0 ? 0 : delay;
    rs_mask = 1u << rs;
    wr_mask = 1u << wr;
    cs_mask = 1u << cs;
    rst_mask = 1u << rst;
    rd_mask = rd >= 0 ? (1u << rd) : 0;

    for (int i = 0; i < 16; i++) {
        data_pin[i] = data_pins[i];
        make_output(data_pins[i]);
    }
    for (int i = 0; i < 5; i++) {
        if (control[i] >= 0) make_output(control[i]);
    }

    for (uint32_t value = 0; value < 65536; value++) {
        uint32_t set = 0, clr = 0;
        for (int bit = 0; bit < 16; bit++) {
            if (value & (1u << bit)) set |= 1u << data_pins[bit];
            else clr |= 1u << data_pins[bit];
        }
        set_lut[value] = set;
        clr_lut[value] = clr;
    }

    /* Idle: WR, RD and RST high, RS on "data". CS low the whole time (the only device on the bus). */
    gpio[GPSET0] = wr_mask | rd_mask | rst_mask;
    rs_data();
    gpio[GPCLR0] = cs_mask;
    return 0;
}

/* 1 = treat RS high as "command" (the module's table says so; the ILI9486 chip says low). */
void ili_set_rs_invert(int invert) {
    rs_invert = invert ? 1 : 0;
    rs_data();
}

/* Hardware reset: RST low, then high, then wait for the panel to wake up. */
void ili_reset(void) {
    gpio[GPSET0] = rst_mask;
    sleep_ms(5);
    gpio[GPCLR0] = rst_mask;
    sleep_ms(20);
    gpio[GPSET0] = rst_mask;
    sleep_ms(150);
}

/* A command byte. */
void ili_command(uint8_t command) {
    rs_command();
    bus_write(command);
    rs_data();
}

/* Parameter bytes for the last command, one per write on D0-D7. */
void ili_data(const uint8_t *bytes, int count) {
    for (int i = 0; i < count; i++) bus_write(bytes[i]);
}

/* RGB565 pixels after a 0x2C (memory write) command, one per write on D0-D15. */
void ili_pixels(const uint16_t *pixels, int count) {
    for (int i = 0; i < count; i++) bus_write(pixels[i]);
}

/* The same colour count times (fast clear). */
void ili_fill(uint16_t color, int count) {
    for (int i = 0; i < count; i++) bus_write(color);
}

/*
 * Sends a command, then reads count bytes back with RD (needs RD wired to a GPIO).
 * The data pins become inputs while reading, so the screen drives them (3.3V on this module).
 * For 0xD3 (read ID4) an ILI9486 answers: dummy, 0x00, 0x94, 0x86.
 * Returns 0, or -1 if RD isn't connected to a GPIO.
 */
int ili_read(uint8_t command, uint8_t *out, int count) {
    if (!rd_mask) return -1;
    ili_command(command);

    for (int i = 0; i < 16; i++) make_input(data_pin[i]);
    for (int n = 0; n < count; n++) {
        gpio[GPCLR0] = rd_mask;
        for (int i = 0; i < 40; i++) (void)gpio[GPLEV0]; /* reads are slow: give it time */
        uint32_t level = gpio[GPLEV0];
        gpio[GPSET0] = rd_mask;
        for (int i = 0; i < 40; i++) (void)gpio[GPLEV0];

        uint8_t value = 0;
        for (int bit = 0; bit < 8; bit++) {
            if (level & (1u << data_pin[bit])) value |= (uint8_t)(1u << bit);
        }
        out[n] = value;
    }
    for (int i = 0; i < 16; i++) make_output(data_pin[i]);
    return 0;
}

void ili_close(void) {
    if (gpio) {
        gpio[GPSET0] = cs_mask;
        munmap((void *)gpio, 4096);
        gpio = 0;
    }
}
