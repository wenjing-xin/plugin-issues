/** @type {import('tailwindcss').Config} */
import { addDynamicIconSelectors } from "@iconify/tailwind";

module.exports = {
  content: ["../../src/main/resources/templates/**/*.html", "./src/**/*.ts"],
  darkMode: ['selector', '[data-color-scheme="dark"]'],
  prefix: "piw-",
  theme: {
    container: {
      padding: {
        'DEFAULT': '1rem',
        'lg': '1.5rem',
        '2xl': '2rem'
      },
      center: true,
    },
    extend: {
      colors: {
        'primary': "hsl(var(--mi-primary-color) / <alpha-value>)",
        'secondary': "hsl(var(--mi-secondary-color) / <alpha-value>)",
        'wechat-background': "rgb(var(--mi-bg-wechat) / <alpha-value>)",
        'wechat-comment': "rgb(var(--mi-bg-wechat-comment) / <alpha-value>)",
        'wechat-text': '#576b95'
      },
      transitionProperty: {
        'block': 'block',
        'bg': 'background',
      },
      animation: {
        fadeIn: 'fadeIn 1.5s ease-in 1',
        leftEnter: 'leftEnter .4s ease-in 1',
        leftLeave: 'leftLeave .9s ease-out 1',
        lightSpeedInLeft: 'lightSpeedInLeft 0.8s ease-in-out forwards',
        lightSpeedInRight: 'lightSpeedInRight 0.8s ease-in-out forwards',
        lightSpeedOutLeft: 'lightSpeedOutLeft 0.8s ease-in-out forwards',
        lightSpeedOutRight: 'lightSpeedOutRight 0.8s ease-in-out forwards',
        lightFadeOutLeft: 'lightFadeOutLeft 1s ease-in',
        lightFadeOutRight: 'lightFadeOutRight 1s ease-in',
        lightFadeInTop: 'lightFadeInTop 2s ease-in-out',
        lightFadeInBottom: 'lightFadeInBottom 2s ease-in-out',
        zoomIn: 'zoomIn 0.8s ease-in both',
        customRotate: 'customRotate 1.5s 1 linear',
        sunRotate: 'sunRotate 0.5s 1 linear',
        sunRollBackRotate: 'sunRollBackRotate 0.5s 1 linear',
        rollBackRotate: 'customRotate 1.5s 1 ease-in-out',
        clipCircleIn: 'clipCircleIn 1.2s ease-in-out both',
        circleBarWavePre: 'circleWavePre 12s linear infinite',
        circleBarWaveAfter: 'circleWave 5s linear infinite',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: 0 },
          '50%': { opacity: 0.5 },
          '100%': { opacity: 1 }
        },
        leftEnter: {
          '0%': { transform: 'translateX(-100%)' },
          '100%': { transform: 'translateX(0)' }
        },
        leftLeave: {
          '0%': { transform: 'translateX(0)' },
          '50%': { transform: 'translateX(-50%)' },
          '100%': { transform: 'translateX(-100%)' }
        },
        lightSpeedInLeft: {
          '0%': { transform: 'translate3d(-100%, 0, 0)', opacity: 0 },
          '100%': { transform: 'translate3d(0, 0, 0)', opacity: 1 },
        },
        lightSpeedInRight: {
          '0%': { transform: 'translate3d(100%, 0, 0)', opacity: 0 },
          '100%': { transform: 'translate3d(0, 0, 0)', opacity: 1 },
        },
        lightSpeedOutLeft: {
          '100%': { transform: 'translate3d(-100%, 0, 0)', opacity: 0 },
          '0%': { transform: 'translate3d(0, 0, 0)', opacity: 1 },
        },
        lightSpeedOutRight: {
          '0%': { transform: 'translate3d(0, 0, 0)', opacity: 1 },
          '100%': { transform: 'translate3d(100%, 0, 0)', opacity: 0 }
        },
        lightFadeOutLeft: {
          '0%': { opacity: 1, transform: 'transform: translate3d(0, 0, 0)' },
          '100%': { opacity: 0, transform: 'transform: translate3d(-100%, 0, 0)' }
        },
        lightFadeOutRight: {
          '0%': { opacity: 1, transform: 'transform: translate3d(0, 0, 0)' },
          '100%': { opacity: 0, transform: 'transform: translate3d(200%, 0, 0)' }
        },
        lightFadeInBottom: {
          '0%': { clipPath: 'polygon(0 100%, 100% 100%, 100% 100%, 0 100%)' },
          '100%': { clipPath: 'polygon(0 0, 100% 0, 100% 100%, 0 100%)' },
        },
        lightFadeInTop: {
          '0%': { clipPath: 'polygon(0 0, 100% 0, 100% 0, 0 0)' },
          '100%': { clipPath: 'polygon(0 0, 100% 0, 100% 100%, 0 100%)' }
        },
        zoomIn: {
          'from': { opacity: 0, transform: 'scale3d(0.5, 0.5, 0.5)' },
          'to': { opacity: 1, transform: 'scale3d(1, 1, 1)' }
        },
        customRotate: {
          '0%': { transform: 'rotate(0deg)' },
          '50%': { transform: 'rotate(180deg)' },
          '100%': { transform: 'rotate(360deg)' }
        },
        sunRotate: {
          '0%': { transform: 'rotate(0deg) scale(1)' },
          '100%': { transform: 'rotate(180deg) scale(1.1)' },
        },
        sunRollBackRotate: {
          '0%': { transform: 'rotate(180deg) scale(1.1)' },
          '100%': { transform: 'rotate(0deg) scale(1)' },
        },
        clipCircleIn: {
          '0%': { 'clip-path': 'circle(0 at 50% 50%)' },
          '100%': { 'clip-path': 'circle(100% at 50% 50%)' }
        },
        circleWavePre: {
          '0%': {
            transform: 'rotate(0deg)'
          },
          '100%': {
            transform: 'rotate(360deg)'
          }
        },
        circleWave: {
          '0%': {
            transform: 'rotate(360deg)',
          },
          '100%': {
            transform: 'rotate(0deg)'
          }
        },
      }
    },
  },
  plugins: [
    require("@tailwindcss/typography"),
    addDynamicIconSelectors(),
  ]
};
