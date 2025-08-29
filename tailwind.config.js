/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./templates/**/*.html",
    "./static/**/*.js",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        'gray-750': '#334155',
      }
    },
  },
  plugins: [],
}