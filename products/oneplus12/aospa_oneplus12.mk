#
# Copyright (C) 2025 Paranoid Android
#
# SPDX-License-Identifier: Apache-2.0
#

ifeq (aospa_oneplus12,$(TARGET_PRODUCT))

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit_only.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/aosp_base_telephony.mk)

# Inherit from the device configuration.
$(call inherit-product, device/oneplus/oneplus12/device.mk)

# Inherit from the AOSPA configuration.
$(call inherit-product, vendor/aospa/target/product/aospa-target.mk)

PRODUCT_NAME := aospa_oneplus12
PRODUCT_DEVICE := oneplus12
PRODUCT_MANUFACTURER := OnePlus
PRODUCT_BRAND := OnePlus
PRODUCT_MODEL := CPH2573

PRODUCT_GMS_CLIENTID_BASE := android-oneplus

PRODUCT_BUILD_PROP_OVERRIDES += \
    DeviceName=OP595DL1 \
    DeviceProduct=CPH2573 \
    SystemDevice=OP595DL1 \
    SystemName=CPH2573

# Boot Animation
TARGET_BOOT_ANIMATION_RES := 1440

endif
