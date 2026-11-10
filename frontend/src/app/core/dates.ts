/**
 * A Date → "YYYY-MM-DD" in the user's LOCAL time zone (the format Spring's LocalDate expects).
 *
 * Don't use date.toISOString().slice(0, 10): toISOString() converts to UTC first, so at
 * 8 pm in Toronto (= 1 am UTC next day) it would return TOMORROW's date.
 */
export function toIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0'); // getMonth() is 0-based
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
