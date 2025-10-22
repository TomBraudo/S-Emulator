/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#38cb82',
        danger: '#d9534f',
        surface: '#F5E6D3',
        muted: '#666666',
      },
    },
  },
  plugins: [],
}

